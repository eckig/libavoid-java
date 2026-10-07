package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestRemoveJunctions
{
    @Test
    public void testRemoveJunctions()
    {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingParameter(Router.RoutingParameter.segmentPenalty, 2);
        router.setRoutingParameter(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.fixedSharedPathPenalty, 9000);
        router.setRoutingParameter(Router.RoutingParameter.portDirectionPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 0);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 16);
        router.setRoutingParameter(Router.RoutingParameter.reverseDirectionPenalty, 0);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, false);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, false);
        router.setRoutingOption(Router.RoutingOption.performUnifyingNudgingPreprocessingStep, true);
        router.setRoutingOption(Router.RoutingOption.nudgeSharedPathsWithCommonEndPoint, true);
        Polygon polygon;
        ConnEnd srcPt;
        ConnEnd dstPt;
        ConnEnd heConnPt;
        Polygon newRoute;
        ShapeConnectionPin connPin = null;

        // shapeRef1
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(450, 330));
        polygon.setPoint(1, new Point(490, 330));
        polygon.setPoint(2, new Point(490, 450));
        polygon.setPoint(3, new Point(450, 450));
        ShapeRef shapeRef1 = new ShapeRef(router, polygon, 1);
        connPin = new ShapeConnectionPin(shapeRef1, 1, 0, 0.333333, true, 10, 0);
        connPin.setExclusive(false);
        connPin = new ShapeConnectionPin(shapeRef1, 2, 0, 0.666667, true, 10, 0);
        connPin.setExclusive(false);
        connPin = new ShapeConnectionPin(shapeRef1, 3, 1, 0.5, true, 10, 0);
        connPin.setExclusive(false);
        connPin = new ShapeConnectionPin(shapeRef1, 4, 0.5, 0, true, 10, 0);
        connPin.setExclusive(false);

        // shapeRef8
        polygon = new Polygon(4);
        polygon.setPoint(0, new Point(502, 237.5));
        polygon.setPoint(1, new Point(502, 262.5));
        polygon.setPoint(2, new Point(438, 262.5));
        polygon.setPoint(3, new Point(438, 237.5));
        ShapeRef shapeRef8 = new ShapeRef(router, polygon, 8);
        connPin = new ShapeConnectionPin(shapeRef8, 1, 0, 0.5, true, 1, 0);
        connPin = new ShapeConnectionPin(shapeRef8, 2, 1, 0.5, true, 1, 0);
        connPin = new ShapeConnectionPin(shapeRef8, 3, 0.5, 1, true, 1, 0);
        connPin = new ShapeConnectionPin(shapeRef8, 4, 0.5, 0, true, 1, 0);

        JunctionRef junctionRef6 = new JunctionRef(router, new Point(470, 300), 6);
        // connRef9
        ConnRef connRef9 = new ConnRef(router, 9);
        srcPt = new ConnEnd(junctionRef6);
        connRef9.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef8, 3);
        connRef9.setDestEndpoint(dstPt);
        connRef9.setRoutingType(ConnType.Orthogonal);

        // connRef10 - WITH BUG
        ConnRef connRef10 = new ConnRef(router, 10);
        srcPt = new ConnEnd(junctionRef6);
        connRef10.setSourceEndpoint(srcPt);
        dstPt = new ConnEnd(shapeRef1, 4);
        connRef10.setDestEndpoint(dstPt);
        connRef10.setRoutingType(ConnType.Orthogonal);

        //    router.deleteConnector(connRef5);
        router.processTransaction();

        ConnRef mergedConn = junctionRef6.removeJunctionAndMergeConnectors();
        router.processTransaction();

        Pair<ConnEnd, ConnEnd> ends = mergedConn.endpointConnEnds();

        // Expected result: output images should be virtually identical.
        // Result: An endpoint seems to be disconnected from shapeRef8.
        boolean isConnected = (ends.first.shape() == shapeRef8) || (ends.second.shape() == shapeRef8);

        assertTrue(isConnected);
//        return isConnected ? 0 : 1;
    }
}
