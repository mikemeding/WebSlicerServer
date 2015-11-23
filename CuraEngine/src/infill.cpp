/** Copyright (C) 2013 David Braam - Released under terms of the AGPLv3 License */
#include "infill.h"
#include "functional"
#include "utils/polygonUtils.h"
#include "utils/AABB.h"
#include "utils/logoutput.h"

namespace cura {

void Infill::generate(Polygons& result_polygons, Polygons& result_lines, Polygons* in_between)
{
    if (in_outline.size() == 0) return;
    if (line_distance == 0) return;
    const Polygons* outline = &in_outline;
    Polygons outline_offsetted;
    switch(pattern)
    {
    case EFillMethod::GRID:
        generateGridInfill(in_outline, outlineOffset, result_lines, extrusion_width, line_distance * 2, infill_overlap, fill_angle);
        break;
    case EFillMethod::LINES:
        generateLineInfill(in_outline, outlineOffset, result_lines, extrusion_width, line_distance, infill_overlap, fill_angle);
        break;
    case EFillMethod::TRIANGLES:
        generateTriangleInfill(in_outline, outlineOffset, result_lines, extrusion_width, line_distance * 3, infill_overlap, fill_angle);
        break;
    case EFillMethod::CONCENTRIC:
        if (outlineOffset != 0)
        {
            PolygonUtils::offsetSafe(in_outline, outlineOffset, extrusion_width, outline_offsetted, avoidOverlappingPerimeters);
            outline = &outline_offsetted;
        }
        if (abs(extrusion_width - line_distance) < 10)
        {
            generateConcentricInfillDense(*outline, result_polygons, in_between, extrusion_width, avoidOverlappingPerimeters);
        }
        else 
        {
            generateConcentricInfill(*outline, result_polygons, line_distance);
        }
        break;
    case EFillMethod::ZIG_ZAG:
        if (outlineOffset != 0)
        {
            PolygonUtils::offsetSafe(in_outline, outlineOffset, extrusion_width, outline_offsetted, avoidOverlappingPerimeters);
            outline = &outline_offsetted;
        }
        generateZigZagInfill(*outline, result_lines, extrusion_width, line_distance, infill_overlap, fill_angle, connect_zigzags, use_endPieces);
        break;
    default:
        logError("Fill pattern has unknown value.\n");
        break;
    }
}

    
      
void generateConcentricInfillDense(Polygons outline, Polygons& result, Polygons* in_between, int extrusionWidth, bool avoidOverlappingPerimeters)
{
    while(outline.size() > 0)
    {
        for (unsigned int polyNr = 0; polyNr < outline.size(); polyNr++)
        {
            PolygonRef r = outline[polyNr];
            result.add(r);
        }
        Polygons next_outline;
        PolygonUtils::offsetExtrusionWidth(outline, true, extrusionWidth, next_outline, in_between, avoidOverlappingPerimeters);
        outline = next_outline;
    } 

}

void generateConcentricInfill(Polygons outline, Polygons& result, int inset_value)
{
    while(outline.size() > 0)
    {
        for (unsigned int polyNr = 0; polyNr < outline.size(); polyNr++)
        {
            PolygonRef r = outline[polyNr];
            result.add(r);
        }
        outline = outline.offset(-inset_value);
    } 
}


void generateGridInfill(const Polygons& in_outline, int outlineOffset, Polygons& result,
                        int extrusionWidth, int lineSpacing, double infillOverlap,
                        double rotation)
{
    generateLineInfill(in_outline, outlineOffset, result, extrusionWidth, lineSpacing,
                       infillOverlap, rotation);
    generateLineInfill(in_outline, outlineOffset, result, extrusionWidth, lineSpacing,
                       infillOverlap, rotation + 90);
}

void generateTriangleInfill(const Polygons& in_outline, int outlineOffset, Polygons& result,
                        int extrusionWidth, int lineSpacing, double infillOverlap,
                        double rotation)
{
    generateLineInfill(in_outline, outlineOffset, result, extrusionWidth, lineSpacing,
                       infillOverlap, rotation);
    generateLineInfill(in_outline, outlineOffset, result, extrusionWidth, lineSpacing,
                       infillOverlap, rotation + 60);
    generateLineInfill(in_outline, outlineOffset, result, extrusionWidth, lineSpacing,
                       infillOverlap, rotation + 120);
}

void addLineInfill(Polygons& result, PointMatrix matrix, int scanline_min_idx, int lineSpacing, AABB boundary, std::vector<std::vector<int64_t> > cutList, int extrusionWidth)
{
    auto addLine = [&](Point from, Point to)
    {            
        PolygonRef p = result.newPoly();
        p.add(matrix.unapply(from));
        p.add(matrix.unapply(to));
    };
    
    auto compare_int64_t = [](const void* a, const void* b)
    {
        int64_t n = (*(int64_t*)a) - (*(int64_t*)b);
        if (n < 0) return -1;
        if (n > 0) return 1;
        return 0;
    };
    
    int scanline_idx = 0;
    for(int64_t x = scanline_min_idx * lineSpacing; x < boundary.max.X; x += lineSpacing)
    {
        qsort(cutList[scanline_idx].data(), cutList[scanline_idx].size(), sizeof(int64_t), compare_int64_t);
        for(unsigned int i = 0; i + 1 < cutList[scanline_idx].size(); i+=2)
        {
            if (cutList[scanline_idx][i+1] - cutList[scanline_idx][i] < extrusionWidth / 5)
                continue;
            addLine(Point(x, cutList[scanline_idx][i]), Point(x, cutList[scanline_idx][i+1]));
        }
        scanline_idx += 1;
    }
}

void generateLineInfill(const Polygons& in_outline, int outlineOffset, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation)
{
    if (lineSpacing == 0) return;
    if (in_outline.size() == 0) return;
    Polygons outline = ((outlineOffset)? in_outline.offset(outlineOffset) : in_outline).offset(extrusionWidth * infillOverlap / 100);
    if (outline.size() == 0) return;
    
    PointMatrix matrix(rotation);
    
    outline.applyMatrix(matrix);

    
    AABB boundary(outline);
    
    int scanline_min_idx = boundary.min.X / lineSpacing;
    int lineCount = (boundary.max.X + (lineSpacing - 1)) / lineSpacing - scanline_min_idx;
  
    std::vector<std::vector<int64_t> > cutList; // mapping from scanline to all intersections with polygon segments
    
    for(int n=0; n<lineCount; n++)
        cutList.push_back(std::vector<int64_t>());
    
    for(unsigned int poly_idx=0; poly_idx < outline.size(); poly_idx++)
    {
        Point p0 = outline[poly_idx][outline[poly_idx].size()-1];
        for(unsigned int i=0; i < outline[poly_idx].size(); i++)
        {
            Point p1 = outline[poly_idx][i];
            int64_t xMin = p1.X, xMax = p0.X;
            if (xMin == xMax) {
                p0 = p1;
                continue; 
            }
            if (xMin > xMax) { xMin = p0.X; xMax = p1.X; }
            
            int scanline_idx0 = (p0.X + ((p0.X > 0)? -1 : -lineSpacing)) / lineSpacing; // -1 cause a linesegment on scanline x counts as belonging to scansegment x-1   ...
            int scanline_idx1 = (p1.X + ((p1.X > 0)? -1 : -lineSpacing)) / lineSpacing; // -linespacing because a line between scanline -n and -n-1 belongs to scansegment -n-1 (for n=positive natural number)
            int direction = 1;
            if (p0.X > p1.X) 
            { 
                direction = -1; 
                scanline_idx1 += 1; // only consider the scanlines in between the scansegments
            } else scanline_idx0 += 1; // only consider the scanlines in between the scansegments
            
            for(int scanline_idx = scanline_idx0; scanline_idx != scanline_idx1+direction; scanline_idx+=direction)
            {
                int x = scanline_idx * lineSpacing;
                int y = p1.Y + (p0.Y - p1.Y) * (x - p1.X) / (p0.X - p1.X);
                cutList[scanline_idx - scanline_min_idx].push_back(y);
            }
            p0 = p1;
        }
    }
    
    addLineInfill(result, matrix, scanline_min_idx, lineSpacing, boundary, cutList, extrusionWidth);
}


void generateZigZagInfill(const Polygons& in_outline, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation, bool connect_zigzags, bool use_endPieces)
{
    if (use_endPieces) return generateZigZagIninfill_endPieces(in_outline, result, extrusionWidth, lineSpacing, infillOverlap, rotation, connect_zigzags);
    else return generateZigZagIninfill_noEndPieces(in_outline, result, extrusionWidth, lineSpacing, infillOverlap, rotation);
}

void generateZigZagIninfill_endPieces(const Polygons& in_outline, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation, bool connect_zigzags)
{
//     if (in_outline.size() == 0) return;
//     Polygons outline = in_outline.offset(extrusionWidth * infillOverlap / 100 - extrusionWidth / 2);
    Polygons empty;
    Polygons outline = in_outline.difference(empty); // copy
    if (outline.size() == 0) return;
    
    PointMatrix matrix(rotation);
    
    outline.applyMatrix(matrix);
    
    auto addLine = [&](Point from, Point to)
    {            
        PolygonRef p = result.newPoly();
        p.add(matrix.unapply(from));
        p.add(matrix.unapply(to));
    };   
        
    AABB boundary(outline);
    
    int scanline_min_idx = boundary.min.X / lineSpacing;
    int lineCount = (boundary.max.X + (lineSpacing - 1)) / lineSpacing - scanline_min_idx;
    
    std::vector<std::vector<int64_t> > cutList; // mapping from scanline to all intersections with polygon segments
    
    for(int n=0; n<lineCount; n++)
        cutList.push_back(std::vector<int64_t>());
    for(unsigned int polyNr=0; polyNr < outline.size(); polyNr++)
    {
        std::vector<Point> firstBoundarySegment;
        std::vector<Point> unevenBoundarySegment; // stored cause for connected_zigzags a boundary segment which ends in an uneven scanline needs to be included
        
        bool isFirstBoundarySegment = true;
        bool firstBoundarySegmentEndsInEven;
        
        bool isEvenScanSegment = false; 
        
        
        Point p0 = outline[polyNr][outline[polyNr].size()-1];
        Point lastPoint = p0;
        for(unsigned int i=0; i < outline[polyNr].size(); i++)
        {
            Point p1 = outline[polyNr][i];
            int64_t xMin = p1.X, xMax = p0.X;
            if (xMin == xMax) {
                lastPoint = p1;
                p0 = p1;
                continue; 
            }
            if (xMin > xMax) { xMin = p0.X; xMax = p1.X; }
            
            int scanline_idx0 = (p0.X + ((p0.X > 0)? -1 : -lineSpacing)) / lineSpacing; // -1 cause a linesegment on scanline x counts as belonging to scansegment x-1   ...
            int scanline_idx1 = (p1.X + ((p1.X > 0)? -1 : -lineSpacing)) / lineSpacing; // -linespacing because a line between scanline -n and -n-1 belongs to scansegment -n-1 (for n=positive natural number)
            int direction = 1;
            if (p0.X > p1.X) 
            { 
                direction = -1; 
                scanline_idx1 += 1; // only consider the scanlines in between the scansegments
            } else scanline_idx0 += 1; // only consider the scanlines in between the scansegments
            
            
            if (isFirstBoundarySegment) firstBoundarySegment.push_back(p0);
            for(int scanline_idx = scanline_idx0; scanline_idx != scanline_idx1+direction; scanline_idx+=direction)
            {
                int x = scanline_idx * lineSpacing;
                int y = p1.Y + (p0.Y - p1.Y) * (x - p1.X) / (p0.X - p1.X);
                cutList[scanline_idx - scanline_min_idx].push_back(y);
                
                
                bool last_isEvenScanSegment = isEvenScanSegment;
                if (scanline_idx % 2 == 0) isEvenScanSegment = true;
                else isEvenScanSegment = false;
                
                if (!isFirstBoundarySegment)
                {
                    if (last_isEvenScanSegment && (connect_zigzags || !isEvenScanSegment))
                        addLine(lastPoint, Point(x,y));
                    else if (connect_zigzags && !last_isEvenScanSegment && !isEvenScanSegment) // if we end an uneven boundary in an uneven segment
                    { // add whole unevenBoundarySegment (including the just obtained point)
                        for (unsigned int p = 1; p < unevenBoundarySegment.size(); p++)
                        {
                            addLine(unevenBoundarySegment[p-1], unevenBoundarySegment[p]);
                        }
                        addLine(unevenBoundarySegment[unevenBoundarySegment.size()-1], Point(x,y));
                        unevenBoundarySegment.clear();
                    } 
                    if (connect_zigzags && last_isEvenScanSegment && !isEvenScanSegment)
                        unevenBoundarySegment.push_back(Point(x,y));
                    else 
                        unevenBoundarySegment.clear();
                        
                }
                lastPoint = Point(x,y);
                
                if (isFirstBoundarySegment) 
                {
                    firstBoundarySegment.emplace_back(x,y);
                    firstBoundarySegmentEndsInEven = isEvenScanSegment;
                    isFirstBoundarySegment = false;
                }
                
            }
            if (!isFirstBoundarySegment)
            {
                if (isEvenScanSegment)
                    addLine(lastPoint, p1);
                else if (connect_zigzags)
                    unevenBoundarySegment.push_back(p1);
            }
            
            lastPoint = p1;
            p0 = p1;
        }
        
        if (isEvenScanSegment || isFirstBoundarySegment || connect_zigzags)
        {
            for (unsigned int i = 1; i < firstBoundarySegment.size() ; i++)
            {
                if (i < firstBoundarySegment.size() - 1 || !firstBoundarySegmentEndsInEven || connect_zigzags) // only add last element if connect_zigzags or boundary segment ends in uneven scanline
                    addLine(firstBoundarySegment[i-1], firstBoundarySegment[i]);
            }   
        }
        else if (!firstBoundarySegmentEndsInEven)
            addLine(firstBoundarySegment[firstBoundarySegment.size()-2], firstBoundarySegment[firstBoundarySegment.size()-1]);
    } 
    
    if (cutList.size() == 0) return;
    if (connect_zigzags && cutList.size() == 1 && cutList[0].size() <= 2) return;  // don't add connection if boundary already contains whole outline!
    
    addLineInfill(result, matrix, scanline_min_idx, lineSpacing, boundary, cutList, extrusionWidth);
}


void generateZigZagIninfill_noEndPieces(const Polygons& in_outline, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation)
{
    if (in_outline.size() == 0) return;
    Polygons outline = in_outline.offset(extrusionWidth * infillOverlap / 100 - extrusionWidth / 2);
    if (outline.size() == 0) return;
    
    PointMatrix matrix(rotation);
    
    outline.applyMatrix(matrix);
    
    auto addLine = [&](Point from, Point to)
    {            
        PolygonRef p = result.newPoly();
        p.add(matrix.unapply(from));
        p.add(matrix.unapply(to));
    };   
        
    AABB boundary(outline);
    
    int scanline_min_idx = boundary.min.X / lineSpacing;
    int lineCount = (boundary.max.X + (lineSpacing - 1)) / lineSpacing - scanline_min_idx;
    
    std::vector<std::vector<int64_t> > cutList; // mapping from scanline to all intersections with polygon segments
    
    for(int n=0; n<lineCount; n++)
        cutList.push_back(std::vector<int64_t>());
    for(unsigned int polyNr=0; polyNr < outline.size(); polyNr++)
    {
        std::vector<Point> firstBoundarySegment;
        std::vector<Point> boundarySegment;
        
        bool isFirstBoundarySegment = true;
        bool firstBoundarySegmentEndsInEven;
        
        bool isEvenScanSegment = false; 
        
        
        Point p0 = outline[polyNr][outline[polyNr].size()-1];
        for(unsigned int i=0; i < outline[polyNr].size(); i++)
        {
            Point p1 = outline[polyNr][i];
            int64_t xMin = p1.X, xMax = p0.X;
            if (xMin == xMax) {
                p0 = p1;
                continue; 
            }
            if (xMin > xMax) { xMin = p0.X; xMax = p1.X; }
            
            int scanline_idx0 = (p0.X + ((p0.X > 0)? -1 : -lineSpacing)) / lineSpacing; // -1 cause a linesegment on scanline x counts as belonging to scansegment x-1   ...
            int scanline_idx1 = (p1.X + ((p1.X > 0)? -1 : -lineSpacing)) / lineSpacing; // -linespacing because a line between scanline -n and -n-1 belongs to scansegment -n-1 (for n=positive natural number)
            int direction = 1;
            if (p0.X > p1.X) 
            { 
                direction = -1; 
                scanline_idx1 += 1; // only consider the scanlines in between the scansegments
            } else scanline_idx0 += 1; // only consider the scanlines in between the scansegments
            
            
            if (isFirstBoundarySegment) firstBoundarySegment.push_back(p0);
            else boundarySegment.push_back(p0);
            for(int scanline_idx = scanline_idx0; scanline_idx != scanline_idx1+direction; scanline_idx+=direction)
            {
                int x = scanline_idx * lineSpacing;
                int y = p1.Y + (p0.Y - p1.Y) * (x - p1.X) / (p0.X - p1.X);
                cutList[scanline_idx - scanline_min_idx].push_back(y);
                
                
                bool last_isEvenScanSegment = isEvenScanSegment;
                if (scanline_idx % 2 == 0) isEvenScanSegment = true;
                else isEvenScanSegment = false;
                
                if (!isFirstBoundarySegment)
                {
                    if (last_isEvenScanSegment && !isEvenScanSegment)
                    { // add whole boundarySegment (including the just obtained point)
                        for (unsigned int p = 1; p < boundarySegment.size(); p++)
                        {
                            addLine(boundarySegment[p-1], boundarySegment[p]);
                        }
                        addLine(boundarySegment[boundarySegment.size()-1], Point(x,y));
                        boundarySegment.clear();
                    } 
                    else if (isEvenScanSegment) // we are either in an end piece or an uneven boundary segment
                    {
                        boundarySegment.clear();
                        boundarySegment.emplace_back(x,y);
                    } else
                        boundarySegment.clear();
                        
                }
                
                if (isFirstBoundarySegment) 
                {
                    firstBoundarySegment.emplace_back(x,y);
                    firstBoundarySegmentEndsInEven = isEvenScanSegment;
                    isFirstBoundarySegment = false;
                    boundarySegment.emplace_back(x,y);
                }
                
            }
            if (!isFirstBoundarySegment && isEvenScanSegment)
                boundarySegment.push_back(p1);
            
            
            p0 = p1;
        }
        
        if (!isFirstBoundarySegment && isEvenScanSegment && !firstBoundarySegmentEndsInEven)
        {
            for (unsigned int i = 1; i < firstBoundarySegment.size() ; i++)
                addLine(firstBoundarySegment[i-1], firstBoundarySegment[i]);
        }
    } 
    

    addLineInfill(result, matrix, scanline_min_idx, lineSpacing, boundary, cutList, extrusionWidth);

}


}//namespace cura
