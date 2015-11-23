/** Copyright (C) 2013 David Braam - Released under terms of the AGPLv3 License */
#include <stdio.h>

#include "utils/gettime.h"
#include "utils/logoutput.h"

#include "slicer.h"
#include "debug.h" // TODO remove

namespace cura {

void SlicerLayer::makePolygons(Mesh* mesh, bool keep_none_closed, bool extensive_stitching)
{
    Polygons openPolygonList;
    
    // connect line segments
    for(unsigned int startSegment=0; startSegment < segmentList.size(); startSegment++)
    {
        if (segmentList[startSegment].addedToPolygon)
            continue;
        
        Polygon poly;
        poly.add(segmentList[startSegment].start);
        
        unsigned int segmentIndex = startSegment;
        bool canClose;
        while(true)
        {
            canClose = false;
            segmentList[segmentIndex].addedToPolygon = true;
            Point p0 = segmentList[segmentIndex].end;
            poly.add(p0);
            int nextIndex = -1;
            const MeshFace& face = mesh->faces[segmentList[segmentIndex].faceIndex];
            for(unsigned int i=0;i<3;i++)
            {
                decltype(face_idx_to_segment_index.begin()) it;
                if (face.connected_face_index[i] > -1 && (it = face_idx_to_segment_index.find(face.connected_face_index[i])) != face_idx_to_segment_index.end())
                {
                    int index = (*it).second;
                    Point p1 = segmentList[index].start;
                    Point diff = p0 - p1;
                    if (shorterThen(diff, MM2INT(0.01)))
                    {
                        if (index == static_cast<int>(startSegment))
                            canClose = true;
                        if (segmentList[index].addedToPolygon)
                            continue;
                        nextIndex = index;
                    }
                }
            }
            if (nextIndex == -1)
                break;
            segmentIndex = nextIndex;
        }
        if (canClose)
            polygonList.add(poly);
        else
            openPolygonList.add(poly);
    }
    //Clear the segmentList to save memory, it is no longer needed after this point.
    segmentList.clear();
    
    // TODO: (?) for mesh surface mode: connect open polygons. Maybe the above algorithm can create two open polygons which are actually connected when the starting segment is in the middle between the two open polygons.

    //Connecting polygons that are not closed yet, as models are not always perfect manifold we need to join some stuff up to get proper polygons
    //First link up polygon ends that are within 2 microns.
    for(unsigned int i=0;i<openPolygonList.size();i++)
    {
        if (openPolygonList[i].size() < 1) continue;
        for(unsigned int j=0;j<openPolygonList.size();j++)
        {
            if (openPolygonList[j].size() < 1) continue;
            
            Point diff = openPolygonList[i][openPolygonList[i].size()-1] - openPolygonList[j][0];
            int64_t distSquared = vSize2(diff);

            if (distSquared < MM2INT(0.02) * MM2INT(0.02))
            {
                if (i == j)
                {
                    polygonList.add(openPolygonList[i]);
                    openPolygonList[i].clear();
                    break;
                }else{
                    for(unsigned int n=0; n<openPolygonList[j].size(); n++)
                        openPolygonList[i].add(openPolygonList[j][n]);

                    openPolygonList[j].clear();
                }
            }
        }
    }

    if (mesh->getSettingAsSurfaceMode("magic_mesh_surface_mode") == ESurfaceMode::NORMAL)
    {
        //Next link up all the missing ends, closing up the smallest gaps first. This is an inefficient implementation which can run in O(n*n*n) time.
        while(1)
        {
        int64_t bestScore = MM2INT(10.0) * MM2INT(10.0);
        unsigned int bestA = -1;
        unsigned int bestB = -1;
        bool reversed = false;
        for(unsigned int i=0;i<openPolygonList.size();i++)
        {
            if (openPolygonList[i].size() < 1) continue;
            for(unsigned int j=0;j<openPolygonList.size();j++)
            {
                if (openPolygonList[j].size() < 1) continue;
                
                Point diff = openPolygonList[i][openPolygonList[i].size()-1] - openPolygonList[j][0];
                int64_t distSquared = vSize2(diff);
                if (distSquared < bestScore)
                {
                    bestScore = distSquared;
                    bestA = i;
                    bestB = j;
                    reversed = false;
                }

                if (i != j)
                {
                    Point diff = openPolygonList[i][openPolygonList[i].size()-1] - openPolygonList[j][openPolygonList[j].size()-1];
                    int64_t distSquared = vSize2(diff);
                    if (distSquared < bestScore)
                    {
                        bestScore = distSquared;
                        bestA = i;
                        bestB = j;
                        reversed = true;
                    }
                }
            }
        }
        
        if (bestScore >= MM2INT(10.0) * MM2INT(10.0))
            break;
        
        if (bestA == bestB)
        {
            polygonList.add(openPolygonList[bestA]);
            openPolygonList[bestA].clear();
        }else{
            if (reversed)
            {
                if (openPolygonList[bestA].polygonLength() > openPolygonList[bestB].polygonLength())
                {
                    for(unsigned int n=openPolygonList[bestB].size()-1; int(n)>=0; n--)
                        openPolygonList[bestA].add(openPolygonList[bestB][n]);
                    openPolygonList[bestB].clear();
                }else{
                    for(unsigned int n=openPolygonList[bestA].size()-1; int(n)>=0; n--)
                        openPolygonList[bestB].add(openPolygonList[bestA][n]);
                    openPolygonList[bestA].clear();
                }
            }else{
                for(unsigned int n=0; n<openPolygonList[bestB].size(); n++)
                    openPolygonList[bestA].add(openPolygonList[bestB][n]);
                openPolygonList[bestB].clear();
            }
        }
    }
    }
    if (extensive_stitching)
    {
        //For extensive stitching find 2 open polygons that are touching 2 closed polygons.
        // Then find the sortest path over this polygon that can be used to connect the open polygons,
        // And generate a path over this shortest bit to link up the 2 open polygons.
        // (If these 2 open polygons are the same polygon, then the final result is a closed polyon)
        
        while(1)
        {
            unsigned int bestA = -1;
            unsigned int bestB = -1;
            GapCloserResult bestResult;
            bestResult.len = POINT_MAX;
            bestResult.polygonIdx = -1;
            bestResult.pointIdxA = -1;
            bestResult.pointIdxB = -1;
            
            for(unsigned int i=0; i<openPolygonList.size(); i++)
            {
                if (openPolygonList[i].size() < 1) continue;
                
                {
                    GapCloserResult res = findPolygonGapCloser(openPolygonList[i][0], openPolygonList[i][openPolygonList[i].size()-1]);
                    if (res.len > 0 && res.len < bestResult.len)
                    {
                        bestA = i;
                        bestB = i;
                        bestResult = res;
                    }
                }

                for(unsigned int j=0; j<openPolygonList.size(); j++)
                {
                    if (openPolygonList[j].size() < 1 || i == j) continue;
                    
                    GapCloserResult res = findPolygonGapCloser(openPolygonList[i][0], openPolygonList[j][openPolygonList[j].size()-1]);
                    if (res.len > 0 && res.len < bestResult.len)
                    {
                        bestA = i;
                        bestB = j;
                        bestResult = res;
                    }
                }
            }
            
            if (bestResult.len < POINT_MAX)
            {
                if (bestA == bestB)
                {
                    if (bestResult.pointIdxA == bestResult.pointIdxB)
                    {
                        polygonList.add(openPolygonList[bestA]);
                        openPolygonList[bestA].clear();
                    }
                    else if (bestResult.AtoB)
                    {
                        PolygonRef poly = polygonList.newPoly();
                        for(unsigned int j = bestResult.pointIdxA; j != bestResult.pointIdxB; j = (j + 1) % polygonList[bestResult.polygonIdx].size())
                            poly.add(polygonList[bestResult.polygonIdx][j]);
                        for(unsigned int j = openPolygonList[bestA].size() - 1; int(j) >= 0; j--)
                            poly.add(openPolygonList[bestA][j]);
                        openPolygonList[bestA].clear();
                    }
                    else
                    {
                        unsigned int n = polygonList.size();
                        polygonList.add(openPolygonList[bestA]);
                        for(unsigned int j = bestResult.pointIdxB; j != bestResult.pointIdxA; j = (j + 1) % polygonList[bestResult.polygonIdx].size())
                            polygonList[n].add(polygonList[bestResult.polygonIdx][j]);
                        openPolygonList[bestA].clear();
                    }
                }
                else
                {
                    if (bestResult.pointIdxA == bestResult.pointIdxB)
                    {
                        for(unsigned int n=0; n<openPolygonList[bestA].size(); n++)
                            openPolygonList[bestB].add(openPolygonList[bestA][n]);
                        openPolygonList[bestA].clear();
                    }
                    else if (bestResult.AtoB)
                    {
                        Polygon poly;
                        for(unsigned int n = bestResult.pointIdxA; n != bestResult.pointIdxB; n = (n + 1) % polygonList[bestResult.polygonIdx].size())
                            poly.add(polygonList[bestResult.polygonIdx][n]);
                        for(unsigned int n=poly.size()-1;int(n) >= 0; n--)
                            openPolygonList[bestB].add(poly[n]);
                        for(unsigned int n=0; n<openPolygonList[bestA].size(); n++)
                            openPolygonList[bestB].add(openPolygonList[bestA][n]);
                        openPolygonList[bestA].clear();
                    }
                    else
                    {
                        for(unsigned int n = bestResult.pointIdxB; n != bestResult.pointIdxA; n = (n + 1) % polygonList[bestResult.polygonIdx].size())
                            openPolygonList[bestB].add(polygonList[bestResult.polygonIdx][n]);
                        for(unsigned int n = openPolygonList[bestA].size() - 1; int(n) >= 0; n--)
                            openPolygonList[bestB].add(openPolygonList[bestA][n]);
                        openPolygonList[bestA].clear();
                    }
                }
            }
            else
            {
                break;
            }
        }
    }

    if (keep_none_closed)
    {
        for(unsigned int n=0; n<openPolygonList.size(); n++)
        {
            if (openPolygonList[n].size() > 0)
                polygonList.add(openPolygonList[n]);
        }
    }
    
    for(unsigned int i=0;i<openPolygonList.size();i++)
    {
        if (openPolygonList[i].size() > 0)
            openPolylines.add(openPolygonList[i]);
    }

    //Remove all the tiny polygons, or polygons that are not closed. As they do not contribute to the actual print.
    int snapDistance = MM2INT(1.0);
    for(unsigned int i=0;i<polygonList.size();i++)
    {
        int length = 0;

        for(unsigned int n=1; n<polygonList[i].size(); n++)
        {
            length += vSize(polygonList[i][n] - polygonList[i][n-1]);
            if (length > snapDistance)
                break;
        }
        if (length < snapDistance)
        {
            polygonList.remove(i);
            i--;
        }
    }

    //Finally optimize all the polygons. Every point removed saves time in the long run.
    polygonList.simplify();
    
    polygonList.removeDegenerateVerts(); // remove verts connected to overlapping line segments
    
    int xy_offset = mesh->getSettingInMicrons("xy_offset");
    if (xy_offset != 0)
    {
        polygonList = polygonList.offset(xy_offset);
    }
}


Slicer::Slicer(Mesh* mesh, int initial, int thickness, int layer_count, bool keep_none_closed, bool extensive_stitching)
{
    assert(layer_count > 0);

    layers.resize(layer_count);
    
    for(int32_t layer_nr = 0; layer_nr < layer_count; layer_nr++)
    {
        layers[layer_nr].z = initial + thickness * layer_nr;
    }
    
    for(unsigned int mesh_idx = 0; mesh_idx < mesh->faces.size(); mesh_idx++)
    {
        MeshFace& face = mesh->faces[mesh_idx];
        Point3 p0 = mesh->vertices[face.vertex_index[0]].p;
        Point3 p1 = mesh->vertices[face.vertex_index[1]].p;
        Point3 p2 = mesh->vertices[face.vertex_index[2]].p;
        int32_t minZ = p0.z;
        int32_t maxZ = p0.z;
        if (p1.z < minZ) minZ = p1.z;
        if (p2.z < minZ) minZ = p2.z;
        if (p1.z > maxZ) maxZ = p1.z;
        if (p2.z > maxZ) maxZ = p2.z;
        int32_t layer_max = (maxZ - initial) / thickness;
        for(int32_t layer_nr = (minZ - initial) / thickness; layer_nr <= layer_max; layer_nr++)
        {
            int32_t z = layer_nr * thickness + initial;
            if (z < minZ) continue;
            if (layer_nr < 0) continue;
            
            SlicerSegment s;
            if (p0.z < z && p1.z >= z && p2.z >= z)
                s = project2D(p0, p2, p1, z);
            else if (p0.z > z && p1.z < z && p2.z < z)
                s = project2D(p0, p1, p2, z);

            else if (p1.z < z && p0.z >= z && p2.z >= z)
                s = project2D(p1, p0, p2, z);
            else if (p1.z > z && p0.z < z && p2.z < z)
                s = project2D(p1, p2, p0, z);

            else if (p2.z < z && p1.z >= z && p0.z >= z)
                s = project2D(p2, p1, p0, z);
            else if (p2.z > z && p1.z < z && p0.z < z)
                s = project2D(p2, p0, p1, z);
            else
            {
                //Not all cases create a segment, because a point of a face could create just a dot, and two touching faces
                //  on the slice would create two segments
                continue;
            }
            layers[layer_nr].face_idx_to_segment_index.insert(std::make_pair(mesh_idx, layers[layer_nr].segmentList.size()));
            s.faceIndex = mesh_idx;
            s.addedToPolygon = false;
            layers[layer_nr].segmentList.push_back(s);
        }
    }
    for(unsigned int layer_nr=0; layer_nr<layers.size(); layer_nr++)
    {
        layers[layer_nr].makePolygons(mesh, keep_none_closed, extensive_stitching);
    }
}

}//namespace cura
