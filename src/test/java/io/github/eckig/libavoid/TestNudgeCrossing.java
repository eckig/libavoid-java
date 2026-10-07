package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestNudgeCrossing
{
    @Test
    public void testNudgeCrossing01()
    {
        Router router = new Router(Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
        router.setRoutingParameter(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingParameter(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.fixedSharedPathPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.portDirectionPenalty, 100);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, true);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, false);

        Polygon poly35 = new Polygon(4);
        poly35.setPoint(0, new Point(616.5, 285));
        poly35.setPoint(1, new Point(616.5, 305));
        poly35.setPoint(2, new Point(584.5, 305));
        poly35.setPoint(3, new Point(584.5, 285));
        new ShapeRef(router, poly35, 35);

        Polygon poly38 = new Polygon(4);
        poly38.setPoint(0, new Point(891.5, 331));
        poly38.setPoint(1, new Point(891.5, 351));
        poly38.setPoint(2, new Point(859.5, 351));
        poly38.setPoint(3, new Point(859.5, 331));
        new ShapeRef(router, poly38, 38);

        Polygon poly39 = new Polygon(4);
        poly39.setPoint(0, new Point(891.5, 229));
        poly39.setPoint(1, new Point(891.5, 249));
        poly39.setPoint(2, new Point(859.5, 249));
        poly39.setPoint(3, new Point(859.5, 229));
        new ShapeRef(router, poly39, 39);

        Polygon poly52 = new Polygon(4);
        poly52.setPoint(0, new Point(317.5, 435));
        poly52.setPoint(1, new Point(317.5, 497));
        poly52.setPoint(2, new Point(255.5, 497));
        poly52.setPoint(3, new Point(255.5, 435));
        new ShapeRef(router, poly52, 52);

        Polygon poly123 = new Polygon(4);
        poly123.setPoint(0, new Point(628.5, 225));
        poly123.setPoint(1, new Point(628.5, 285));
        poly123.setPoint(2, new Point(572.5, 285));
        poly123.setPoint(3, new Point(572.5, 225));
        new ShapeRef(router, poly123, 123);

        Polygon poly162 = new Polygon(4);
        poly162.setPoint(0, new Point(630.5, 295));
        poly162.setPoint(1, new Point(630.5, 562));
        poly162.setPoint(2, new Point(562.5, 562));
        poly162.setPoint(3, new Point(562.5, 295));
        new ShapeRef(router, poly162, 162);

        Polygon poly163 = new Polygon(4);
        poly163.setPoint(0, new Point(537.5, 542.75));
        poly163.setPoint(1, new Point(537.5, 734.75));
        poly163.setPoint(2, new Point(469.5, 734.75));
        poly163.setPoint(3, new Point(469.5, 542.75));
        new ShapeRef(router, poly163, 163);

        ConnRef connRef219 = new ConnRef(router, 219);
        ConnEnd srcPt219 = new ConnEnd(new Point(860.5, 341), 4);
        connRef219.setSourceEndpoint(srcPt219);
        ConnEnd dstPt219 = new ConnEnd(new Point(286.5, 466), 15);
        connRef219.setDestEndpoint(dstPt219);
        connRef219.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef220 = new ConnRef(router, 220);
        ConnEnd srcPt220 = new ConnEnd(new Point(860.5, 239), 4);
        connRef220.setSourceEndpoint(srcPt220);
        ConnEnd dstPt220 = new ConnEnd(new Point(286.5, 466), 15);
        connRef220.setDestEndpoint(dstPt220);
        connRef220.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();

        boolean optimisedForConnectorType = false;
        int crossings = router.existsCrossings(optimisedForConnectorType);
        assertEquals(0, crossings);
    }
}
