package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestInlineOverlap
{
    @Test
    public void testInlineOverlap01()
    {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 20);
        Rectangle rect335855988 = new Rectangle(new Point(51140, 50190), new Point(51510, 50885));
        new ShapeRef(router, rect335855988, 335855988);
        Rectangle rect548374163 = new Rectangle(new Point(51915, 50415), new Point(52285, 50885));
        new ShapeRef(router, rect548374163, 548374163);
        Rectangle rect942733064 = new Rectangle(new Point(51771, 50790), new Point(51829, 50885));
        new ShapeRef(router, rect942733064, 942733064);
        ConnEnd srcPt100850179 = new ConnEnd(new Point(51925, 50800), 4);
        ConnEnd dstPt100850179 = new ConnEnd(new Point(51500, 50650), 8);
        new ConnRef(router, srcPt100850179, dstPt100850179, 100850179);
        ConnEnd srcPt240732432 = new ConnEnd(new Point(51925, 50650), 4);
        ConnEnd dstPt240732432 = new ConnEnd(new Point(51800, 50800), 1);
        new ConnRef(router, srcPt240732432, dstPt240732432, 240732432);
        router.processTransaction();
        boolean overlap = router.existsOrthogonalTouchingPaths();
        assertFalse(overlap);
    }

    @Test
    public void testInlineOverlap09()
    {
        Router router = new Router(Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
        router.setRoutingParameter(Router.RoutingParameter.segmentPenalty, 10);
        router.setRoutingParameter(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingParameter(Router.RoutingParameter.crossingPenalty, 100);
        router.setRoutingParameter(Router.RoutingParameter.fixedSharedPathPenalty, 1000);
        router.setRoutingParameter(Router.RoutingParameter.portDirectionPenalty, 100);
        router.setRoutingParameter(Router.RoutingParameter.shapeBufferDistance, 20);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 40);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalSegmentsConnectedToShapes, false);
        router.setRoutingOption(Router.RoutingOption.penaliseOrthogonalSharedPathsAtConnEnds, false);
        router.setRoutingOption(Router.RoutingOption.nudgeOrthogonalTouchingColinearSegments, false);
        Polygon poly1 = new Polygon(4);
        poly1.setPoint(0, new Point(3.43711e+006, 6.73107e+006));
        poly1.setPoint(1, new Point(3.43711e+006, 6.73108e+006));
        poly1.setPoint(2, new Point(3.4371e+006, 6.73108e+006));
        poly1.setPoint(3, new Point(3.4371e+006, 6.73107e+006));
        new ShapeRef(router, poly1, 1);

        Polygon poly2 = new Polygon(4);
        poly2.setPoint(0, new Point(3.43536e+006, 6.73107e+006));
        poly2.setPoint(1, new Point(3.43536e+006, 6.73108e+006));
        poly2.setPoint(2, new Point(3.43535e+006, 6.73108e+006));
        poly2.setPoint(3, new Point(3.43535e+006, 6.73107e+006));
        new ShapeRef(router, poly2, 2);

        Polygon poly3 = new Polygon(4);
        poly3.setPoint(0, new Point(3.43786e+006, 6.73107e+006));
        poly3.setPoint(1, new Point(3.43786e+006, 6.73108e+006));
        poly3.setPoint(2, new Point(3.43785e+006, 6.73108e+006));
        poly3.setPoint(3, new Point(3.43785e+006, 6.73107e+006));
        new ShapeRef(router, poly3, 3);

        Polygon poly4 = new Polygon(4);
        poly4.setPoint(0, new Point(3.43736e+006, 6.73107e+006));
        poly4.setPoint(1, new Point(3.43736e+006, 6.73108e+006));
        poly4.setPoint(2, new Point(3.43735e+006, 6.73108e+006));
        poly4.setPoint(3, new Point(3.43735e+006, 6.73107e+006));
        new ShapeRef(router, poly4, 4);

        Polygon poly5 = new Polygon(4);
        poly5.setPoint(0, new Point(3.4349e+006, 6.73107e+006));
        poly5.setPoint(1, new Point(3.4349e+006, 6.73108e+006));
        poly5.setPoint(2, new Point(3.43489e+006, 6.73108e+006));
        poly5.setPoint(3, new Point(3.43489e+006, 6.73107e+006));
        new ShapeRef(router, poly5, 5);

        Polygon poly6 = new Polygon(4);
        poly6.setPoint(0, new Point(3.43573e+006, 6.73052e+006));
        poly6.setPoint(1, new Point(3.43573e+006, 6.73053e+006));
        poly6.setPoint(2, new Point(3.43572e+006, 6.73053e+006));
        poly6.setPoint(3, new Point(3.43572e+006, 6.73052e+006));
        new ShapeRef(router, poly6, 6);

        Polygon poly7 = new Polygon(4);
        poly7.setPoint(0, new Point(3.43636e+006, 6.73107e+006));
        poly7.setPoint(1, new Point(3.43636e+006, 6.73108e+006));
        poly7.setPoint(2, new Point(3.43635e+006, 6.73108e+006));
        poly7.setPoint(3, new Point(3.43635e+006, 6.73107e+006));
        new ShapeRef(router, poly7, 7);

        Polygon poly8 = new Polygon(4);
        poly8.setPoint(0, new Point(3.43769e+006, 6.73107e+006));
        poly8.setPoint(1, new Point(3.43769e+006, 6.73108e+006));
        poly8.setPoint(2, new Point(3.43768e+006, 6.73108e+006));
        poly8.setPoint(3, new Point(3.43768e+006, 6.73107e+006));
        new ShapeRef(router, poly8, 8);

        Polygon poly9 = new Polygon(4);
        poly9.setPoint(0, new Point(3.43761e+006, 6.73107e+006));
        poly9.setPoint(1, new Point(3.43761e+006, 6.73108e+006));
        poly9.setPoint(2, new Point(3.4376e+006, 6.73108e+006));
        poly9.setPoint(3, new Point(3.4376e+006, 6.73107e+006));
        new ShapeRef(router, poly9, 9);

        Polygon poly10 = new Polygon(4);
        poly10.setPoint(0, new Point(3.43661e+006, 6.73107e+006));
        poly10.setPoint(1, new Point(3.43661e+006, 6.73108e+006));
        poly10.setPoint(2, new Point(3.4366e+006, 6.73108e+006));
        poly10.setPoint(3, new Point(3.4366e+006, 6.73107e+006));
        new ShapeRef(router, poly10, 10);

        Polygon poly11 = new Polygon(4);
        poly11.setPoint(0, new Point(3.4369e+006, 6.73107e+006));
        poly11.setPoint(1, new Point(3.4369e+006, 6.73108e+006));
        poly11.setPoint(2, new Point(3.43689e+006, 6.73108e+006));
        poly11.setPoint(3, new Point(3.43689e+006, 6.73107e+006));
        new ShapeRef(router, poly11, 11);

        Polygon poly12 = new Polygon(4);
        poly12.setPoint(0, new Point(3.43619e+006, 6.73107e+006));
        poly12.setPoint(1, new Point(3.43619e+006, 6.73108e+006));
        poly12.setPoint(2, new Point(3.43618e+006, 6.73108e+006));
        poly12.setPoint(3, new Point(3.43618e+006, 6.73107e+006));
        new ShapeRef(router, poly12, 12);

        Polygon poly13 = new Polygon(4);
        poly13.setPoint(0, new Point(3.43611e+006, 6.73107e+006));
        poly13.setPoint(1, new Point(3.43611e+006, 6.73108e+006));
        poly13.setPoint(2, new Point(3.4361e+006, 6.73108e+006));
        poly13.setPoint(3, new Point(3.4361e+006, 6.73107e+006));
        new ShapeRef(router, poly13, 13);

        Polygon poly14 = new Polygon(4);
        poly14.setPoint(0, new Point(3.43461e+006, 6.73107e+006));
        poly14.setPoint(1, new Point(3.43461e+006, 6.73108e+006));
        poly14.setPoint(2, new Point(3.4346e+006, 6.73108e+006));
        poly14.setPoint(3, new Point(3.4346e+006, 6.73107e+006));
        new ShapeRef(router, poly14, 14);

        Polygon poly15 = new Polygon(4);
        poly15.setPoint(0, new Point(3.43476e+006, 6.73128e+006));
        poly15.setPoint(1, new Point(3.43476e+006, 6.73148e+006));
        poly15.setPoint(2, new Point(3.43456e+006, 6.73148e+006));
        poly15.setPoint(3, new Point(3.43456e+006, 6.73128e+006));
        new ShapeRef(router, poly15, 15);

        Polygon poly16 = new Polygon(4);
        poly16.setPoint(0, new Point(3.43476e+006, 6.73013e+006));
        poly16.setPoint(1, new Point(3.43476e+006, 6.73033e+006));
        poly16.setPoint(2, new Point(3.43456e+006, 6.73033e+006));
        poly16.setPoint(3, new Point(3.43456e+006, 6.73013e+006));
        new ShapeRef(router, poly16, 16);

        Polygon poly17 = new Polygon(4);
        poly17.setPoint(0, new Point(3.43501e+006, 6.73013e+006));
        poly17.setPoint(1, new Point(3.43501e+006, 6.73033e+006));
        poly17.setPoint(2, new Point(3.43481e+006, 6.73033e+006));
        poly17.setPoint(3, new Point(3.43481e+006, 6.73013e+006));
        new ShapeRef(router, poly17, 17);

        Polygon poly18 = new Polygon(4);
        poly18.setPoint(0, new Point(3.43526e+006, 6.73013e+006));
        poly18.setPoint(1, new Point(3.43526e+006, 6.73033e+006));
        poly18.setPoint(2, new Point(3.43506e+006, 6.73033e+006));
        poly18.setPoint(3, new Point(3.43506e+006, 6.73013e+006));
        new ShapeRef(router, poly18, 18);

        Polygon poly19 = new Polygon(4);
        poly19.setPoint(0, new Point(3.43551e+006, 6.73013e+006));
        poly19.setPoint(1, new Point(3.43551e+006, 6.73033e+006));
        poly19.setPoint(2, new Point(3.43531e+006, 6.73033e+006));
        poly19.setPoint(3, new Point(3.43531e+006, 6.73013e+006));
        new ShapeRef(router, poly19, 19);

        Polygon poly20 = new Polygon(4);
        poly20.setPoint(0, new Point(3.43501e+006, 6.73128e+006));
        poly20.setPoint(1, new Point(3.43501e+006, 6.73148e+006));
        poly20.setPoint(2, new Point(3.43481e+006, 6.73148e+006));
        poly20.setPoint(3, new Point(3.43481e+006, 6.73128e+006));
        new ShapeRef(router, poly20, 20);

        Polygon poly21 = new Polygon(4);
        poly21.setPoint(0, new Point(3.43526e+006, 6.73128e+006));
        poly21.setPoint(1, new Point(3.43526e+006, 6.73148e+006));
        poly21.setPoint(2, new Point(3.43506e+006, 6.73148e+006));
        poly21.setPoint(3, new Point(3.43506e+006, 6.73128e+006));
        new ShapeRef(router, poly21, 21);

        Polygon poly22 = new Polygon(4);
        poly22.setPoint(0, new Point(3.43551e+006, 6.73128e+006));
        poly22.setPoint(1, new Point(3.43551e+006, 6.73148e+006));
        poly22.setPoint(2, new Point(3.43531e+006, 6.73148e+006));
        poly22.setPoint(3, new Point(3.43531e+006, 6.73128e+006));
        new ShapeRef(router, poly22, 22);

        Polygon poly23 = new Polygon(4);
        poly23.setPoint(0, new Point(3.43576e+006, 6.73128e+006));
        poly23.setPoint(1, new Point(3.43576e+006, 6.73148e+006));
        poly23.setPoint(2, new Point(3.43556e+006, 6.73148e+006));
        poly23.setPoint(3, new Point(3.43556e+006, 6.73128e+006));
        new ShapeRef(router, poly23, 23);

        Polygon poly24 = new Polygon(4);
        poly24.setPoint(0, new Point(3.43601e+006, 6.73128e+006));
        poly24.setPoint(1, new Point(3.43601e+006, 6.73148e+006));
        poly24.setPoint(2, new Point(3.43581e+006, 6.73148e+006));
        poly24.setPoint(3, new Point(3.43581e+006, 6.73128e+006));
        new ShapeRef(router, poly24, 24);

        Polygon poly25 = new Polygon(4);
        poly25.setPoint(0, new Point(3.43626e+006, 6.73128e+006));
        poly25.setPoint(1, new Point(3.43626e+006, 6.73148e+006));
        poly25.setPoint(2, new Point(3.43606e+006, 6.73148e+006));
        poly25.setPoint(3, new Point(3.43606e+006, 6.73128e+006));
        new ShapeRef(router, poly25, 25);

        Polygon poly26 = new Polygon(4);
        poly26.setPoint(0, new Point(3.43651e+006, 6.73128e+006));
        poly26.setPoint(1, new Point(3.43651e+006, 6.73148e+006));
        poly26.setPoint(2, new Point(3.43631e+006, 6.73148e+006));
        poly26.setPoint(3, new Point(3.43631e+006, 6.73128e+006));
        new ShapeRef(router, poly26, 26);

        Polygon poly27 = new Polygon(4);
        poly27.setPoint(0, new Point(3.43676e+006, 6.73128e+006));
        poly27.setPoint(1, new Point(3.43676e+006, 6.73148e+006));
        poly27.setPoint(2, new Point(3.43656e+006, 6.73148e+006));
        poly27.setPoint(3, new Point(3.43656e+006, 6.73128e+006));
        new ShapeRef(router, poly27, 27);

        Polygon poly28 = new Polygon(4);
        poly28.setPoint(0, new Point(3.43701e+006, 6.73128e+006));
        poly28.setPoint(1, new Point(3.43701e+006, 6.73148e+006));
        poly28.setPoint(2, new Point(3.43681e+006, 6.73148e+006));
        poly28.setPoint(3, new Point(3.43681e+006, 6.73128e+006));
        new ShapeRef(router, poly28, 28);

        Polygon poly29 = new Polygon(4);
        poly29.setPoint(0, new Point(3.43726e+006, 6.73128e+006));
        poly29.setPoint(1, new Point(3.43726e+006, 6.73148e+006));
        poly29.setPoint(2, new Point(3.43706e+006, 6.73148e+006));
        poly29.setPoint(3, new Point(3.43706e+006, 6.73128e+006));
        new ShapeRef(router, poly29, 29);

        Polygon poly30 = new Polygon(4);
        poly30.setPoint(0, new Point(3.43751e+006, 6.73128e+006));
        poly30.setPoint(1, new Point(3.43751e+006, 6.73148e+006));
        poly30.setPoint(2, new Point(3.43731e+006, 6.73148e+006));
        poly30.setPoint(3, new Point(3.43731e+006, 6.73128e+006));
        new ShapeRef(router, poly30, 30);

        Polygon poly31 = new Polygon(4);
        poly31.setPoint(0, new Point(3.43776e+006, 6.73128e+006));
        poly31.setPoint(1, new Point(3.43776e+006, 6.73148e+006));
        poly31.setPoint(2, new Point(3.43756e+006, 6.73148e+006));
        poly31.setPoint(3, new Point(3.43756e+006, 6.73128e+006));
        new ShapeRef(router, poly31, 31);

        Polygon poly32 = new Polygon(4);
        poly32.setPoint(0, new Point(3.43576e+006, 6.73013e+006));
        poly32.setPoint(1, new Point(3.43576e+006, 6.73033e+006));
        poly32.setPoint(2, new Point(3.43556e+006, 6.73033e+006));
        poly32.setPoint(3, new Point(3.43556e+006, 6.73013e+006));
        new ShapeRef(router, poly32, 32);

        Polygon poly33 = new Polygon(4);
        poly33.setPoint(0, new Point(3.43801e+006, 6.73128e+006));
        poly33.setPoint(1, new Point(3.43801e+006, 6.73148e+006));
        poly33.setPoint(2, new Point(3.43781e+006, 6.73148e+006));
        poly33.setPoint(3, new Point(3.43781e+006, 6.73128e+006));
        new ShapeRef(router, poly33, 33);

        ConnRef connRef34 = new ConnRef(router, 34);
        ConnEnd srcPt34 = new ConnEnd(new Point(3.4351e+006, 6.73033e+006), 2);
        connRef34.setSourceEndpoint(srcPt34);
        ConnEnd dstPt34 = new ConnEnd(new Point(3.4356e+006, 6.73128e+006), 1);
        connRef34.setDestEndpoint(dstPt34);
        connRef34.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef35 = new ConnRef(router, 35);
        ConnEnd srcPt35 = new ConnEnd(new Point(3.43485e+006, 6.73033e+006), 2);
        connRef35.setSourceEndpoint(srcPt35);
        ConnEnd dstPt35 = new ConnEnd(new Point(3.4351e+006, 6.73128e+006), 1);
        connRef35.setDestEndpoint(dstPt35);
        connRef35.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef36 = new ConnRef(router, 36);
        ConnEnd srcPt36 = new ConnEnd(new Point(3.4346e+006, 6.73033e+006), 2);
        connRef36.setSourceEndpoint(srcPt36);
        ConnEnd dstPt36 = new ConnEnd(new Point(3.43485e+006, 6.73128e+006), 1);
        connRef36.setDestEndpoint(dstPt36);
        connRef36.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef37 = new ConnRef(router, 37);
        ConnEnd srcPt37 = new ConnEnd(new Point(3.4356e+006, 6.73033e+006), 2);
        connRef37.setSourceEndpoint(srcPt37);
        ConnEnd dstPt37 = new ConnEnd(new Point(3.43685e+006, 6.73128e+006), 1);
        connRef37.setDestEndpoint(dstPt37);
        connRef37.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef38 = new ConnRef(router, 38);
        ConnEnd srcPt38 = new ConnEnd(new Point(3.43564e+006, 6.73033e+006), 2);
        connRef38.setSourceEndpoint(srcPt38);
        ConnEnd dstPt38 = new ConnEnd(new Point(3.43764e+006, 6.73128e+006), 1);
        connRef38.setDestEndpoint(dstPt38);
        connRef38.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef39 = new ConnRef(router, 39);
        ConnEnd srcPt39 = new ConnEnd(new Point(3.43568e+006, 6.73033e+006), 2);
        connRef39.setSourceEndpoint(srcPt39);
        ConnEnd dstPt39 = new ConnEnd(new Point(3.43614e+006, 6.73128e+006), 1);
        connRef39.setDestEndpoint(dstPt39);
        connRef39.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef40 = new ConnRef(router, 40);
        ConnEnd srcPt40 = new ConnEnd(new Point(3.43572e+006, 6.73033e+006), 2);
        connRef40.setSourceEndpoint(srcPt40);
        ConnEnd dstPt40 = new ConnEnd(new Point(3.43572e+006, 6.73052e+006), 1);
        connRef40.setDestEndpoint(dstPt40);
        connRef40.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef41 = new ConnRef(router, 41);
        ConnEnd srcPt41 = new ConnEnd(new Point(3.43535e+006, 6.73033e+006), 2);
        connRef41.setSourceEndpoint(srcPt41);
        ConnEnd dstPt41 = new ConnEnd(new Point(3.43585e+006, 6.73128e+006), 1);
        connRef41.setDestEndpoint(dstPt41);
        connRef41.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef42 = new ConnRef(router, 42);
        ConnEnd srcPt42 = new ConnEnd(new Point(3.43535e+006, 6.73128e+006), 1);
        connRef42.setSourceEndpoint(srcPt42);
        ConnEnd dstPt42 = new ConnEnd(new Point(3.43535e+006, 6.73108e+006), 2);
        connRef42.setDestEndpoint(dstPt42);
        connRef42.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef43 = new ConnRef(router, 43);
        ConnEnd srcPt43 = new ConnEnd(new Point(3.4366e+006, 6.73128e+006), 1);
        connRef43.setSourceEndpoint(srcPt43);
        ConnEnd dstPt43 = new ConnEnd(new Point(3.4366e+006, 6.73108e+006), 2);
        connRef43.setDestEndpoint(dstPt43);
        connRef43.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef44 = new ConnRef(router, 44);
        ConnEnd srcPt44 = new ConnEnd(new Point(3.43489e+006, 6.73128e+006), 1);
        connRef44.setSourceEndpoint(srcPt44);
        ConnEnd dstPt44 = new ConnEnd(new Point(3.43489e+006, 6.73108e+006), 2);
        connRef44.setDestEndpoint(dstPt44);
        connRef44.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef45 = new ConnRef(router, 45);
        ConnEnd srcPt45 = new ConnEnd(new Point(3.43689e+006, 6.73128e+006), 1);
        connRef45.setSourceEndpoint(srcPt45);
        ConnEnd dstPt45 = new ConnEnd(new Point(3.43689e+006, 6.73108e+006), 2);
        connRef45.setDestEndpoint(dstPt45);
        connRef45.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef46 = new ConnRef(router, 46);
        ConnEnd srcPt46 = new ConnEnd(new Point(3.4361e+006, 6.73128e+006), 1);
        connRef46.setSourceEndpoint(srcPt46);
        ConnEnd dstPt46 = new ConnEnd(new Point(3.4361e+006, 6.73108e+006), 2);
        connRef46.setDestEndpoint(dstPt46);
        connRef46.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef47 = new ConnRef(router, 47);
        ConnEnd srcPt47 = new ConnEnd(new Point(3.43618e+006, 6.73128e+006), 1);
        connRef47.setSourceEndpoint(srcPt47);
        ConnEnd dstPt47 = new ConnEnd(new Point(3.43618e+006, 6.73108e+006), 2);
        connRef47.setDestEndpoint(dstPt47);
        connRef47.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef48 = new ConnRef(router, 48);
        ConnEnd srcPt48 = new ConnEnd(new Point(3.4371e+006, 6.73128e+006), 1);
        connRef48.setSourceEndpoint(srcPt48);
        ConnEnd dstPt48 = new ConnEnd(new Point(3.4371e+006, 6.73108e+006), 2);
        connRef48.setDestEndpoint(dstPt48);
        connRef48.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef49 = new ConnRef(router, 49);
        ConnEnd srcPt49 = new ConnEnd(new Point(3.43735e+006, 6.73128e+006), 1);
        connRef49.setSourceEndpoint(srcPt49);
        ConnEnd dstPt49 = new ConnEnd(new Point(3.43735e+006, 6.73108e+006), 2);
        connRef49.setDestEndpoint(dstPt49);
        connRef49.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef50 = new ConnRef(router, 50);
        ConnEnd srcPt50 = new ConnEnd(new Point(3.4376e+006, 6.73128e+006), 1);
        connRef50.setSourceEndpoint(srcPt50);
        ConnEnd dstPt50 = new ConnEnd(new Point(3.4376e+006, 6.73108e+006), 2);
        connRef50.setDestEndpoint(dstPt50);
        connRef50.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef51 = new ConnRef(router, 51);
        ConnEnd srcPt51 = new ConnEnd(new Point(3.43768e+006, 6.73128e+006), 1);
        connRef51.setSourceEndpoint(srcPt51);
        ConnEnd dstPt51 = new ConnEnd(new Point(3.43768e+006, 6.73108e+006), 2);
        connRef51.setDestEndpoint(dstPt51);
        connRef51.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef52 = new ConnRef(router, 52);
        ConnEnd srcPt52 = new ConnEnd(new Point(3.4346e+006, 6.73128e+006), 1);
        connRef52.setSourceEndpoint(srcPt52);
        ConnEnd dstPt52 = new ConnEnd(new Point(3.4346e+006, 6.73108e+006), 2);
        connRef52.setDestEndpoint(dstPt52);
        connRef52.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef53 = new ConnRef(router, 53);
        ConnEnd srcPt53 = new ConnEnd(new Point(3.43635e+006, 6.73128e+006), 1);
        connRef53.setSourceEndpoint(srcPt53);
        ConnEnd dstPt53 = new ConnEnd(new Point(3.43635e+006, 6.73108e+006), 2);
        connRef53.setDestEndpoint(dstPt53);
        connRef53.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef54 = new ConnRef(router, 54);
        ConnEnd srcPt54 = new ConnEnd(new Point(3.43785e+006, 6.73128e+006), 1);
        connRef54.setSourceEndpoint(srcPt54);
        ConnEnd dstPt54 = new ConnEnd(new Point(3.43785e+006, 6.73108e+006), 2);
        connRef54.setDestEndpoint(dstPt54);
        connRef54.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();
        boolean overlap = router.existsOrthogonalFixedSegmentOverlap();
        assertFalse(overlap);
    }
}
