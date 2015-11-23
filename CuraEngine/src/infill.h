/** Copyright (C) 2013 David Braam - Released under terms of the AGPLv3 License */
#ifndef INFILL_H
#define INFILL_H

#include "utils/polygon.h"
#include "settings.h"

namespace cura
{
    class Infill 
    {
        EFillMethod pattern;
        const Polygons& in_outline;
        int outlineOffset;
        bool avoidOverlappingPerimeters;
        int extrusion_width;
        int line_distance;
        double infill_overlap;
        double fill_angle;
        bool connect_zigzags;
        bool use_endPieces;

    public:
        Infill(EFillMethod pattern, const Polygons& in_outline, int outlineOffset, bool avoidOverlappingPerimeters, int extrusion_width, int line_distance, double infill_overlap, double fill_angle, bool connect_zigzags, bool use_endPieces)
        : pattern(pattern)
        , in_outline(in_outline)
        , outlineOffset(outlineOffset)
        , avoidOverlappingPerimeters(avoidOverlappingPerimeters)
        , extrusion_width(extrusion_width)
        , line_distance(line_distance)
        , infill_overlap(infill_overlap)
        , fill_angle(fill_angle)
        , connect_zigzags(connect_zigzags)
        , use_endPieces(use_endPieces)
        {
        }
        void generate(Polygons& result_polygons, Polygons& result_lines, Polygons* in_between);
    };

    void generateInfill(EFillMethod pattern, const Polygons& in_outline, int outlineOffset, Polygons& result_polygons, Polygons& result_lines, Polygons* in_between, bool avoidOverlappingPerimeters, int extrusion_width, int line_distance, double infill_overlap, double fill_angle, bool connect_zigzags, bool use_endPieces);

    void generateConcentricInfill(Polygons outline, Polygons& result, int inset_value);

    void generateConcentricInfillDense(Polygons outline, Polygons& result, Polygons* in_between, int extrusionWidth, bool avoidOverlappingPerimeters);

    void generateGridInfill(const Polygons& in_outline, int outlineOffset, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation);

    void generateTriangleInfill(const Polygons& in_outline, int outlineOffset, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation);

    /*!
     * generate lines within the area of \p in_outline, at regular intervals of \p lineSpacing
     * 
     * idea:
     * intersect a regular grid of 'scanlines' with the area inside \p in_outline
     * 
     * we call the areas between two consecutive scanlines a 'scansegment'.
     * Scansegment x is the area between scanline x and scanline x+1
     * 
     * algorithm:
     * 1) for each line segment of each polygon:
     *      store the intersections of that line segment with all scanlines in a mapping (vector of vectors) from scanline to intersections
     *      (zigzag): add boundary segments to result
     * 2) for each scanline:
     *      sort the associated intersections 
     *      and connect them using the even-odd rule
     * 
     */
    void generateLineInfill(const Polygons& in_outline, int outlineOffset, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation);

    void generateZigZagInfill(const Polygons& in_outline, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation, bool connect_zigzags, bool use_endPieces);

    /*!
     * adapted from generateLineInfill(.)
     * 
     * generate lines within the area of [in_outline], at regular intervals of [lineSpacing]
     * idea:
     * intersect a regular grid of 'scanlines' with the area inside [in_outline]
     * sigzag:
     * include pieces of boundary, connecting the lines, forming an accordion like zigzag instead of separate lines    |_|^|_|
     * 
     * we call the areas between two consecutive scanlines a 'scansegment'
     * 
     * algorithm:
     * 1. for each line segment of each polygon:
     *      store the intersections of that line segment with all scanlines in a mapping (vector of vectors) from scanline to intersections
     *      (zigzag): add boundary segments to result
     * 2. for each scanline:
     *      sort the associated intersections 
     *      and connect them using the even-odd rule
     * 
     * zigzag algorithm:
     * while walking around (each) polygon (1.)
     *  if polygon intersects with even scanline
     *      start boundary segment (add each following segment to the [result])
     *  when polygon intersects with a scanline again
     *      stop boundary segment (stop adding segments to the [result])
     *      if polygon intersects with even scanline again (instead of odd)
     *          dont add the last line segment to the boundary (unless [connect_zigzags])
     * 
     * 
     *     <--
     *     ___
     *    |   |   |
     *    |   |   |
     *    |   |___|
     *         -->
     * 
     *        ^ = even scanline
     * 
     * start boundary from even scanline! :D
     * 
     * 
     *          _____
     *   |     |     | ,
     *   |     |     |  |
     *   |_____|     |__/
     * 
     *   ^     ^     ^    scanlines
     *                 ^  disconnected end piece
     */
    void generateZigZagIninfill_endPieces(const Polygons& in_outline, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation, bool connect_zigzags);

    void generateZigZagIninfill_noEndPieces(const Polygons& in_outline, Polygons& result, int extrusionWidth, int lineSpacing, double infillOverlap, double rotation);
}//namespace cura

#endif//INFILL_H
