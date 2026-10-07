package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestCppPortfinalSegmentNudging
{
    @Test
    public void testfinalSegmentNudging1()
    {
    Router router = new Router(
            Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
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
    ConnEnd srcPt416 = new ConnEnd(new Point(2041.79, 1872.95), 15);
    connRef416.setSourceEndpoint(srcPt416);
    ConnEnd dstPt416 = new ConnEnd(new Point(2156.79, 1872.95), 4);
    connRef416.setDestEndpoint(dstPt416);
    connRef416.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef531 = new ConnRef(router, 531);
    ConnEnd srcPt531 = new ConnEnd(new Point(2186.79, 1872.95), 8);
    connRef531.setSourceEndpoint(srcPt531);
    ConnEnd dstPt531 = new ConnEnd(new Point(2241.79, 1872.95), 15);
    connRef531.setDestEndpoint(dstPt531);
    connRef531.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef726 = new ConnRef(router, 726);
    ConnEnd srcPt726 = new ConnEnd(new Point(2111.79, 1884.95), 15);
    connRef726.setSourceEndpoint(srcPt726);
    ConnEnd dstPt726 = new ConnEnd(new Point(2241.79, 1872.95), 15);
    connRef726.setDestEndpoint(dstPt726);
    connRef726.setRoutingType(ConnType.Orthogonal);
    router.processTransaction();
    boolean optimisedForConnectorType = true;
    int crossings = router.existsCrossings(optimisedForConnectorType);
    assertFalse(crossings > 0);
    }

    @Test
    public void testfinalSegmentNudging2()
    {
        Router router = new Router(
                Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.portDirectionPenalty, 100);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, true);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);
        Polygon poly3 = new Polygon(4);
        poly3.setPoint(0, new Point(328, 368));
        poly3.setPoint(1, new Point(328, 400));
        poly3.setPoint(2, new Point(308, 400));
        poly3.setPoint(3, new Point(308, 368));
        new ShapeRef(router, poly3, 3);
        Polygon poly4 = new Polygon(4);
        poly4.setPoint(0, new Point(328, 492));
        poly4.setPoint(1, new Point(328, 524));
        poly4.setPoint(2, new Point(308, 524));
        poly4.setPoint(3, new Point(308, 492));
        new ShapeRef(router, poly4, 4);
        Polygon poly13 = new Polygon(4);
        poly13.setPoint(0, new Point(349, 415));
        poly13.setPoint(1, new Point(349, 477));
        poly13.setPoint(2, new Point(287, 477));
        poly13.setPoint(3, new Point(287, 415));
        new ShapeRef(router, poly13, 13);
        Polygon poly17 = new Polygon(4);
        poly17.setPoint(0, new Point(640.857, 336));
        poly17.setPoint(1, new Point(640.857, 398));
        poly17.setPoint(2, new Point(578.857, 398));
        poly17.setPoint(3, new Point(578.857, 336));
        new ShapeRef(router, poly17, 17);
        Polygon poly18 = new Polygon(4);
        poly18.setPoint(0, new Point(404, 364));
        poly18.setPoint(1, new Point(404, 404));
        poly18.setPoint(2, new Point(328, 404));
        poly18.setPoint(3, new Point(328, 364));
        new ShapeRef(router, poly18, 18);
        Polygon poly19 = new Polygon(4);
        poly19.setPoint(0, new Point(404, 488));
        poly19.setPoint(1, new Point(404, 528));
        poly19.setPoint(2, new Point(328, 528));
        poly19.setPoint(3, new Point(328, 488));
        new ShapeRef(router, poly19, 19);
        Polygon poly26 = new Polygon(4);
        poly26.setPoint(0, new Point(632.857, 419.5));
        poly26.setPoint(1, new Point(632.857, 459.5));
        poly26.setPoint(2, new Point(572.857, 459.5));
        poly26.setPoint(3, new Point(572.857, 419.5));
        new ShapeRef(router, poly26, 26);
        ConnRef connRef30 = new ConnRef(router, 30);
        ConnEnd srcPt30 = new ConnEnd(new Point(318, 523), 2);
        connRef30.setSourceEndpoint(srcPt30);
        ConnEnd dstPt30 = new ConnEnd(new Point(609.857, 367), 15);
        connRef30.setDestEndpoint(dstPt30);
        connRef30.setRoutingType(ConnType.Orthogonal);
        ConnRef connRef46 = new ConnRef(router, 46);
        ConnEnd srcPt46 = new ConnEnd(new Point(609.857, 367), 15);
        connRef46.setSourceEndpoint(srcPt46);
        ConnEnd dstPt46 = new ConnEnd(new Point(318, 369), 1);
        connRef46.setDestEndpoint(dstPt46);
        connRef46.setRoutingType(ConnType.Orthogonal);
        ConnRef connRef50 = new ConnRef(router, 50);
        ConnEnd srcPt50 = new ConnEnd(new Point(609.857, 367), 15);
        connRef50.setSourceEndpoint(srcPt50);
        ConnEnd dstPt50 = new ConnEnd(new Point(577.857, 439.5), 4);
        connRef50.setDestEndpoint(dstPt50);
        connRef50.setRoutingType(ConnType.Orthogonal);
        router.processTransaction();
        boolean atEnds = true;
        boolean overlap = router.existsOrthogonalFixedSegmentOverlap(atEnds);
        assertFalse(overlap);
    }

    @Test
    public void testfinalSegmentNudging3()
    {
        Router router = new Router(
                Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
        router.setRoutingParameter(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingParameter(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.fixedSharedPathPenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.portDirectionPenalty, 100);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, true);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, false);
        Polygon poly13 = new Polygon(4);
        poly13.setPoint(0, new Point(851.5, 1998.92));
        poly13.setPoint(1, new Point(851.5, 2018.92));
        poly13.setPoint(2, new Point(819.5, 2018.92));
        poly13.setPoint(3, new Point(819.5, 1998.92));
        new ShapeRef(router, poly13, 13);
        Polygon poly65 = new Polygon(4);
        poly65.setPoint(0, new Point(998.5, 1977.92));
        poly65.setPoint(1, new Point(998.5, 2039.92));
        poly65.setPoint(2, new Point(936.5, 2039.92));
        poly65.setPoint(3, new Point(936.5, 1977.92));
        new ShapeRef(router, poly65, 65);
        Polygon poly68 = new Polygon(4);
        poly68.setPoint(0, new Point(610.5, 1977.92));
        poly68.setPoint(1, new Point(610.5, 2039.92));
        poly68.setPoint(2, new Point(548.5, 2039.92));
        poly68.setPoint(3, new Point(548.5, 1977.92));
        new ShapeRef(router, poly68, 68);
        Polygon poly94 = new Polygon(4);
        poly94.setPoint(0, new Point(998.5, 1861.92));
        poly94.setPoint(1, new Point(998.5, 1923.92));
        poly94.setPoint(2, new Point(936.5, 1923.92));
        poly94.setPoint(3, new Point(936.5, 1861.92));
        new ShapeRef(router, poly94, 94);
        Polygon poly104 = new Polygon(4);
        poly104.setPoint(0, new Point(863.5, 1938.92));
        poly104.setPoint(1, new Point(863.5, 1998.92));
        poly104.setPoint(2, new Point(807.5, 1998.92));
        poly104.setPoint(3, new Point(807.5, 1938.92));
        new ShapeRef(router, poly104, 104);
        ConnRef connRef183 = new ConnRef(router, 183);
        ConnEnd srcPt183 = new ConnEnd(new Point(820.5, 2008.92), 4);
        connRef183.setSourceEndpoint(srcPt183);
        ConnEnd dstPt183 = new ConnEnd(new Point(579.5, 2008.92), 15);
        connRef183.setDestEndpoint(dstPt183);
        connRef183.setRoutingType(ConnType.Orthogonal);
        ConnRef connRef184 = new ConnRef(router, 184);
        ConnEnd srcPt184 = new ConnEnd(new Point(820.5, 2008.92), 4);
        connRef184.setSourceEndpoint(srcPt184);
        ConnEnd dstPt184 = new ConnEnd(new Point(967.5, 1892.92), 15);
        connRef184.setDestEndpoint(dstPt184);
        connRef184.setRoutingType(ConnType.Orthogonal);
        router.processTransaction();
        boolean optimisedForConnectorType = false;
        int crossings = router.existsCrossings(optimisedForConnectorType);
        assertFalse(crossings > 0);
    }

}
