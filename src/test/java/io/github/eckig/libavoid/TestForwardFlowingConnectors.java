package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestForwardFlowingConnectors
{
    @Test
    public void testForwardFlowingConnectors01()
    {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingParameter(Router.RoutingParameter.segmentPenalty, 10);
        router.setRoutingParameter(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.crossingPenalty, 1000);
        router.setRoutingParameter(Router.RoutingParameter.fixedSharedPathPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.portDirectionPenalty, 100);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 1);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 10);
        router.setRoutingParameter(Router.RoutingParameter.reverseDirectionPenalty, 500);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, false);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, false);
        router.setRoutingOption(Router.RoutingOption.performUnifyingNudgingPreprocessingStep, true);
        Polygon polygon;
        ConnRef connRef;
        ConnEnd srcPt;
        ConnEnd dstPt;
        // shapeRef1
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(0, 0));
        polygon.setPoint(1, new Point(0, 0));
        polygon.setPoint(2, new Point(0, 0));
        polygon.setPoint(3, new Point(0, 0));
        new ShapeRef(router, polygon, 1);

        // shapeRef2
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(0, 0));
        polygon.setPoint(1, new Point(0, 0));
        polygon.setPoint(2, new Point(0, 0));
        polygon.setPoint(3, new Point(0, 0));
        new ShapeRef(router, polygon, 2);

        // shapeRef3
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(0, 0));
        polygon.setPoint(1, new Point(0, 0));
        polygon.setPoint(2, new Point(0, 0));
        polygon.setPoint(3, new Point(0, 0));
        new ShapeRef(router, polygon, 3);

        // shapeRef4
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(0, 0));
        polygon.setPoint(1, new Point(0, 0));
        polygon.setPoint(2, new Point(0, 0));
        polygon.setPoint(3, new Point(0, 0));
        new ShapeRef(router, polygon, 4);

        // shapeRef5
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(501, 345));
        polygon.setPoint(1, new Point(501, 404));
        polygon.setPoint(2, new Point(421, 404));
        polygon.setPoint(3, new Point(421, 345));
        ShapeRef shapeRef5 = new ShapeRef(router, polygon, 5);
        new ShapeConnectionPin(shapeRef5, 5, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef5, 6, 0, 0.79096, true, 0, ConnDirFlag.ConnDirLeft);
        new ShapeConnectionPin(shapeRef5, 7, 0, 0.514124, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef6
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(94, 251.5));
        polygon.setPoint(1, new Point(94, 315.5));
        polygon.setPoint(2, new Point(12, 315.5));
        polygon.setPoint(3, new Point(12, 251.5));
        ShapeRef shapeRef6 = new ShapeRef(router, polygon, 6);
        new ShapeConnectionPin(shapeRef6, 8, 1, 0.640625, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef6, 9, 0, 0.640625, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef7
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(634.366, 262));
        polygon.setPoint(1, new Point(634.366, 305));
        polygon.setPoint(2, new Point(416.366, 305));
        polygon.setPoint(3, new Point(416.366, 262));
        ShapeRef shapeRef7 = new ShapeRef(router, polygon, 7);
        new ShapeConnectionPin(shapeRef7, 10, 1, 0.709302, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef7, 11, 0, 0.709302, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef8
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(324, 147.167));
        polygon.setPoint(1, new Point(324, 206.167));
        polygon.setPoint(2, new Point(236, 206.167));
        polygon.setPoint(3, new Point(236, 147.167));
        ShapeRef shapeRef8 = new ShapeRef(router, polygon, 8);
        new ShapeConnectionPin(shapeRef8, 12, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef8, 13, 0, 0.79096, true, 0, ConnDirFlag.ConnDirLeft);
        new ShapeConnectionPin(shapeRef8, 14, 0, 0.514124, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef9
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(816, 353.167));
        polygon.setPoint(1, new Point(816, 412.167));
        polygon.setPoint(2, new Point(735, 412.167));
        polygon.setPoint(3, new Point(735, 353.167));
        ShapeRef shapeRef9 = new ShapeRef(router, polygon, 9);
        new ShapeConnectionPin(shapeRef9, 15, 1, 0.514124, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef9, 16, 1, 0.79096, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef9, 17, 0, 0.79096, true, 0, ConnDirFlag.ConnDirLeft);
        new ShapeConnectionPin(shapeRef9, 18, 0, 0.514124, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef10
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(981, 263.833));
        polygon.setPoint(1, new Point(981, 321.833));
        polygon.setPoint(2, new Point(828, 321.833));
        polygon.setPoint(3, new Point(828, 263.833));
        ShapeRef shapeRef10 = new ShapeRef(router, polygon, 10);
        new ShapeConnectionPin(shapeRef10, 19, 0, 0.655172, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef11
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(1011.49, 361.833));
        polygon.setPoint(1, new Point(1011.49, 419.833));
        polygon.setPoint(2, new Point(834.489, 419.833));
        polygon.setPoint(3, new Point(834.489, 361.833));
        ShapeRef shapeRef11 = new ShapeRef(router, polygon, 11);
        new ShapeConnectionPin(shapeRef11, 20, 0, 0.655172, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef12
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(511, 155.333));
        polygon.setPoint(1, new Point(511, 214.333));
        polygon.setPoint(2, new Point(422, 214.333));
        polygon.setPoint(3, new Point(422, 155.333));
        ShapeRef shapeRef12 = new ShapeRef(router, polygon, 12);
        new ShapeConnectionPin(shapeRef12, 21, 1, 0.514124, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef12, 22, 1, 0.79096, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef12, 23, 0, 0.79096, true, 0, ConnDirFlag.ConnDirLeft);
        new ShapeConnectionPin(shapeRef12, 24, 0, 0.514124, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef13
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(690, 66));
        polygon.setPoint(1, new Point(690, 124));
        polygon.setPoint(2, new Point(523, 124));
        polygon.setPoint(3, new Point(523, 66));
        ShapeRef shapeRef13 = new ShapeRef(router, polygon, 13);
        new ShapeConnectionPin(shapeRef13, 25, 0, 0.655172, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef14
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(720.212, 164));
        polygon.setPoint(1, new Point(720.212, 222));
        polygon.setPoint(2, new Point(529.212, 222));
        polygon.setPoint(3, new Point(529.212, 164));
        ShapeRef shapeRef14 = new ShapeRef(router, polygon, 14);
        new ShapeConnectionPin(shapeRef14, 26, 0, 0.655172, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef15
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(217, 336.833));
        polygon.setPoint(1, new Point(217, 395.833));
        polygon.setPoint(2, new Point(98, 395.833));
        polygon.setPoint(3, new Point(98, 336.833));
        ShapeRef shapeRef15 = new ShapeRef(router, polygon, 15);
        new ShapeConnectionPin(shapeRef15, 27, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef15, 28, 0, 0.652542, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef16
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(413, 147.167));
        polygon.setPoint(1, new Point(413, 206.167));
        polygon.setPoint(2, new Point(336, 206.167));
        polygon.setPoint(3, new Point(336, 147.167));
        ShapeRef shapeRef16 = new ShapeRef(router, polygon, 16);
        new ShapeConnectionPin(shapeRef16, 29, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef16, 30, 0, 0.652542, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef17
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(306, 336.833));
        polygon.setPoint(1, new Point(306, 395.833));
        polygon.setPoint(2, new Point(229, 395.833));
        polygon.setPoint(3, new Point(229, 336.833));
        ShapeRef shapeRef17 = new ShapeRef(router, polygon, 17);
        new ShapeConnectionPin(shapeRef17, 31, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef17, 32, 0, 0.652542, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef18
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(175, 139));
        polygon.setPoint(1, new Point(175, 198));
        polygon.setPoint(2, new Point(98, 198));
        polygon.setPoint(3, new Point(98, 139));
        ShapeRef shapeRef18 = new ShapeRef(router, polygon, 18);
        new ShapeConnectionPin(shapeRef18, 33, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef18, 34, 0, 0.652542, true, 0, ConnDirFlag.ConnDirLeft);

        // shapeRef19
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(409, 399.333));
        polygon.setPoint(1, new Point(409, 458.333));
        polygon.setPoint(2, new Point(298, 458.333));
        polygon.setPoint(3, new Point(298, 399.333));
        ShapeRef shapeRef19 = new ShapeRef(router, polygon, 19);
        new ShapeConnectionPin(shapeRef19, 35, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);

        // shapeRef20
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(224, 40));
        polygon.setPoint(1, new Point(224, 99));
        polygon.setPoint(2, new Point(106, 99));
        polygon.setPoint(3, new Point(106, 40));
        ShapeRef shapeRef20 = new ShapeRef(router, polygon, 20);
        new ShapeConnectionPin(shapeRef20, 36, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);

        // shapeRef21
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(604, 345));
        polygon.setPoint(1, new Point(604, 404));
        polygon.setPoint(2, new Point(513, 404));
        polygon.setPoint(3, new Point(513, 345));
        ShapeRef shapeRef21 = new ShapeRef(router, polygon, 21);
        new ShapeConnectionPin(shapeRef21, 37, 1, 0.652542, true, 0, ConnDirFlag.ConnDirRight);
        new ShapeConnectionPin(shapeRef21, 38, 0, 0.652542, true, 0, ConnDirFlag.ConnDirLeft);

        // connRef1
        connRef = new ConnRef(router, 1);
        srcPt = new ConnEnd(shapeRef5, 5);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef21, 38);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef2
        connRef = new ConnRef(router, 2);
        srcPt = new ConnEnd(shapeRef6, 8);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef18, 34);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef3
        connRef = new ConnRef(router, 3);
        srcPt = new ConnEnd(shapeRef6, 8);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef15, 28);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef4
        connRef = new ConnRef(router, 4);
        srcPt = new ConnEnd(shapeRef6, 8);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef12, 23);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef5
        connRef = new ConnRef(router, 5);
        srcPt = new ConnEnd(shapeRef6, 8);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef7, 11);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef6
        connRef = new ConnRef(router, 6);
        srcPt = new ConnEnd(shapeRef7, 10);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef9, 17);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);
        ConnRef connector6 = connRef;

        // connRef7
        connRef = new ConnRef(router, 7);
        srcPt = new ConnEnd(shapeRef8, 12);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef16, 30);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef8
        connRef = new ConnRef(router, 8);
        srcPt = new ConnEnd(shapeRef9, 15);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef10, 19);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef9
        connRef = new ConnRef(router, 9);
        srcPt = new ConnEnd(shapeRef9, 16);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef11, 20);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef10
        connRef = new ConnRef(router, 10);
        srcPt = new ConnEnd(shapeRef12, 21);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef13, 25);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef11
        connRef = new ConnRef(router, 11);
        srcPt = new ConnEnd(shapeRef12, 22);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef14, 26);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef12
        connRef = new ConnRef(router, 12);
        srcPt = new ConnEnd(shapeRef15, 27);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef17, 32);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef13
        connRef = new ConnRef(router, 13);
        srcPt = new ConnEnd(shapeRef16, 29);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef12, 24);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef14
        connRef = new ConnRef(router, 14);
        srcPt = new ConnEnd(shapeRef17, 31);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef5, 7);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef15
        connRef = new ConnRef(router, 15);
        srcPt = new ConnEnd(shapeRef18, 33);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef8, 14);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef16
        connRef = new ConnRef(router, 16);
        srcPt = new ConnEnd(shapeRef19, 35);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef5, 7);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef17
        connRef = new ConnRef(router, 17);
        srcPt = new ConnEnd(shapeRef20, 36);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef8, 14);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        // connRef18
        connRef = new ConnRef(router, 18);
        srcPt = new ConnEnd(shapeRef21, 37);
        connRef.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef9, 18);
        connRef.setDestEndpoint(dstPt);
        connRef.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();
        // Test that connector 6 has three segments and doesnt loop right
        // around the shapes on the right due to the crossing penalty.
        boolean suceeds = (connector6.displayRoute().size() == 4);

        //router.outputDiagram("output/forwardFlowingConnectors01");
        assertTrue(suceeds);
//        return (suceeds ? 0 : 1);
    }
}
