/*
 * @author Guido Daniel Wolff
 * Copyright 2021, 2022 CompuPhase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package packagemanager;
/* 1 */
import java.lang.Math;
import java.util.ArrayList;

/* 2 */
/**
 *
 * @author Thiadmer Riemersma
 *
 * This is a class for simple transformations of the vertices of a polygon, like
 * moving and rotating a polygon. The purpose is to ease handling pads with
 * rotation and origin offsets. In the application, rotations are restricted to
 * multiples of 90 degrees, and pads are limited by basic shapes. Because of
 * these limitations, we can basically represent each pad by its bounding box
 * (i.e. a rectangle).
 *
 * The steps to calculate the transformed dimensions of a pad, are:
 *
 * 1) Polygon padPoly = Polygon.FromRect(pad.width, pad.length);
 *
 *      This creates a rectangular polygon (with given width/length) that is
 *      centered. So x1 = -width/2, and x2 = +width/2;
 *
 * 2) padPoly.Move(-pad.originX, -pad.goriginY);
 *
 *      This sets the origin of the pad, by moving all points in the opposite
 *      direction.
 *
 * 3) padPoly.Rotate(Package.orientationAsInt(pin.rotation));
 *
 *      Now apply rotation. Since the pad must be rotated around its origin, it
 *      is important to do this step after setting the origin.
 *
 * 4) padPoly.Move(pin.xPos, pin.yPos);
 *
 *      Finally, move the pad to the location of the pin. Of course, if the
 *      pad rotation is zero, the two Move() operations can be combined, but if
 *      there is a rotation (and especially if the pad has an origin offset plus
 *      a rotation), this is no longer the case.
 *
 * 5) padPoly.Scale(scaleFactor);
 *
 *      If needed to scale the polygon from its original dimensions (in mm) to
 *      the size of the preview image.
 *
 * 6) padPoly.Flip(Polygon.FLIP_Y);
 *
 *      To mirror the polygon, for example when the Y-axis points downwards,
 *      such as (typically) in computer graphics.
 *
 * 7) double padLeft = padPoly.Left();
 *
 *      Get the x-coordinate of the pad after its transformation. Likewise,
 *      there are functions to get the Right(), Top() and Bottom() values. Note
 *      that Left() must always be smaller than Right() and that Bottom() must
 *      always be smaller than Top(). For the polygon class, the Y-axis points
 *      upwards.
 */

public class Polygon {

    class Vertex {
        double x, y;
        Vertex(double _x, double _y) {
            this.x = _x;
            this.y = _y;
        }
        Vertex(Vertex pt){
            this.x = pt.x;
            this.y = pt.y;
        }
    }

    ArrayList<Vertex> vertices;
    double bbox_left, bbox_right, bbox_top, bbox_bottom;

    Polygon(){
        vertices = new ArrayList<>();
        bbox_left = bbox_right = bbox_top = bbox_bottom = 0;
    }
    Polygon(Polygon p){
        vertices = new ArrayList<>();
        for(Vertex pt : p.vertices){
            this.vertices.add(new Vertex(pt));
        }
        UpdateBoundingBox();
    }

    public void AddVertex(double x, double y){
        /* you can construct any shape of polygon by adding its vertices
         * individually
         */
        vertices.add(new Vertex(x, y));
        UpdateBoundingBox();
    }

    public void AppendPolygon(Polygon p){
        /* this is only useful for calculating bounding boxes of multiple
         * polygons, because the points are not sorted (and the polygon may
         * therefore not be a valid shape)
         */
        for(Vertex pt : p.vertices){
            vertices.add(new Vertex(pt));
        }
        UpdateBoundingBox();
    }

    public static Polygon FromRect(double width, double height){
        /* a rectangle centred on its geometric centre */
        Polygon p = new Polygon();
        p.AddVertex(-width/2, -height/2);
        p.AddVertex(width/2, -height/2);
        p.AddVertex(width/2, height/2);
        p.AddVertex(-width/2, height/2);
        return p;
    }

    public void Move(double x, double y){
        /* for setting the origin, move the polygon in the opposite direction;
         * so to move the origin to (-2, 1), call Move(2, -1)
        */
        for(Vertex pt : vertices){
            pt.x += x;
            pt.y += y;
        }
        UpdateBoundingBox();
    }

    public void Rotate(double angle){
        /* angle in degrees (counter-clockwize), rotation around the origin */
        double c = Math.cos(angle * Math.PI / 180);
        double s = Math.sin(angle * Math.PI / 180);
        for(Vertex pt : vertices){
            double new_x = pt.x * c - pt.y * s;
            double new_y = pt.x * s + pt.y * c;
            pt.x = new_x;
            pt.y = new_y;
        }
        UpdateBoundingBox();
    }

    public void Scale(double factor){
        /* scale around the origin */
        for(Vertex pt : vertices){
            pt.x *= factor;
            pt.y *= factor;
        }
        UpdateBoundingBox();
    }

    public enum FlipType{
        FLIP_X, /* horizontal flip (left-right) */
        FLIP_Y  /* vertical flip (up-down) */
    }

    public void Flip(FlipType type){
        if(type == FlipType.FLIP_X){
            for(Vertex pt : vertices){
                pt.x = -pt.x;
            }
        } else {
            assert (type == FlipType.FLIP_Y);
            for(Vertex pt : vertices){
                pt.y = -pt.y;
            }
        }
        UpdateBoundingBox();
    }

    public double Left(){
        return bbox_left;   /* bounding box, left edge */
    }
    public double Right(){
        return bbox_right;  /* bounding box, right edge */
    }
    public double Bottom(){
        return bbox_bottom; /* bounding box, bottom edge */
    }
    public double Top(){
        return bbox_top;    /* bounding box, top edge */
    }

    private void UpdateBoundingBox(){
        if(vertices.size() > 0){
            bbox_left = bbox_right = vertices.get(0).x;
            bbox_top = bbox_bottom = vertices.get(0).y;
            for(Vertex pt : vertices){
                if(pt.x > bbox_right)
                    bbox_right = pt.x;
                else if(pt.x < bbox_left)
                    bbox_left = pt.x;
                if(pt.y > bbox_top)
                    bbox_top = pt.y;
                else if(pt.y < bbox_bottom)
                    bbox_bottom = pt.y;
            }
        } else {
            bbox_left = bbox_right = bbox_top = bbox_bottom = 0;
        }
    }
}
