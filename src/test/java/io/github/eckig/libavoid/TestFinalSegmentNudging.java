package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestFinalSegmentNudging
{
    @Test
    public void test()
    {
        Router router = new Router(Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.portDirectionPenalty, 100);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, true);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);

        Polygon poly12 = new Polygon(4);

        poly12.setPoint(0, new Point(2072.79, 1841.95));
        poly12.setPoint(1, new Point(2072.79, 1903.95));
        poly12.setPoint(2, new Point(2010.79, 1903.95));
        poly12.setPoint(3, new Point(2010.79, 1841.95));
        new ShapeRef(router, poly12, 12);

        Polygon poly13 = new Polygon(4);
        poly13.setPoint(0, new Point(2272.79, 1841.95));
        poly13.setPoint(1, new Point(2272.79, 1903.95));
        poly13.setPoint(2, new Point(2210.79, 1903.95));
        poly13.setPoint(3, new Point(2210.79, 1841.95));
        new ShapeRef(router, poly13, 13);


        Polygon poly96 = new Polygon(4);
        poly96.setPoint(0, new Point(2187.79, 1862.95));
        poly96.setPoint(1, new Point(2187.79, 1882.95));
        poly96.setPoint(2, new Point(2155.79, 1882.95));
        poly96.setPoint(3, new Point(2155.79, 1862.95));
        new ShapeRef(router, poly96, 96);

        Polygon poly97 = new Polygon(4);
        poly97.setPoint(0, new Point(2139.79, 1864.95));
        poly97.setPoint(1, new Point(2139.79, 1904.95));
        poly97.setPoint(2, new Point(2063.79, 1904.95));
        poly97.setPoint(3, new Point(2063.79, 1864.95));
        new ShapeRef(router, poly97, 97);

        Polygon poly250 = new Polygon(4);
        poly250.setPoint(0, new Point(2199.79, 1802.95));
        poly250.setPoint(1, new Point(2199.79, 1862.95));
        poly250.setPoint(2, new Point(2143.79, 1862.95));
        poly250.setPoint(3, new Point(2143.79, 1802.95));
        new ShapeRef(router, poly250, 250);

        ConnRef connRef416 = new ConnRef(router, 416);
        ConnEnd srcPt416=new ConnEnd(new Point(2041.79, 1872.95), 15);
        connRef416.setSourceEndpoint(srcPt416);
        ConnEnd dstPt416=new ConnEnd(new Point(2156.79, 1872.95), 4);
        connRef416.setDestEndpoint(dstPt416);
        connRef416.setRoutingType(ConnType.Orthogonal);


        ConnRef connRef531 = new ConnRef(router, 531);
        ConnEnd srcPt531=new ConnEnd(new Point(2186.79, 1872.95), 8);
        connRef531.setSourceEndpoint(srcPt531);
        ConnEnd dstPt531=new ConnEnd(new Point(2241.79, 1872.95), 15);
        connRef531.setDestEndpoint(dstPt531);
        connRef531.setRoutingType(ConnType.Orthogonal);


        ConnRef connRef726 = new ConnRef(router, 726);
        ConnEnd srcPt726=new ConnEnd(new Point(2111.79, 1884.95), 15);
        connRef726.setSourceEndpoint(srcPt726);
        ConnEnd dstPt726=new ConnEnd(new Point(2241.79, 1872.95), 15);
        connRef726.setDestEndpoint(dstPt726);
        connRef726.setRoutingType(ConnType.Orthogonal);


        router.processTransaction();

        boolean optimisedForConnectorType = true;
        int crossings = router.existsCrossings(optimisedForConnectorType);

        assertFalse(crossings > 0);
    }
}
