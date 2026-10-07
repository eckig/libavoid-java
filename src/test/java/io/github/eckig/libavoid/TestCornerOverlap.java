package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestCornerOverlap
{
    @Test
    public void test()
    {
        Router router = new Router(Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 110);
        router.setOrthogonalNudgeDistance(12);

        Polygon poly21424960 = new Polygon(4);

        poly21424960.setPoint(0, new Point(29950, 27210));
        poly21424960.setPoint(1, new Point(29950, 27290));
        poly21424960.setPoint(2, new Point(29700, 27290));
        poly21424960.setPoint(3, new Point(29700, 27210));
        new ShapeRef(router, poly21424960, 21424960);

        Polygon poly435105790 = new Polygon(4);
        poly435105790.setPoint(0, new Point(29950, 27110));
        poly435105790.setPoint(1, new Point(29950, 27190));
        poly435105790.setPoint(2, new Point(29700, 27190));
        poly435105790.setPoint(3, new Point(29700, 27110));
        new ShapeRef(router, poly435105790, 435105790);

        Polygon poly40787191 = new Polygon(4);
        poly40787191.setPoint(0, new Point(29950, 27010));
        poly40787191.setPoint(1, new Point(29950, 27090));
        poly40787191.setPoint(2, new Point(29700, 27090));
        poly40787191.setPoint(3, new Point(29700, 27010));
        new ShapeRef(router, poly40787191, 40787191);

        Polygon poly76911264 = new Polygon(4);
        poly76911264.setPoint(0, new Point(29950, 26910));
        poly76911264.setPoint(1, new Point(29950, 26990));
        poly76911264.setPoint(2, new Point(29700, 26990));
        poly76911264.setPoint(3, new Point(29700, 26910));
        new ShapeRef(router, poly76911264, 76911264);

        Polygon poly506213376 = new Polygon(4);
        poly506213376.setPoint(0, new Point(29925, 26820));
        poly506213376.setPoint(1, new Point(29925, 26880));
        poly506213376.setPoint(2, new Point(29675, 26880));
        poly506213376.setPoint(3, new Point(29675, 26820));
        new ShapeRef(router, poly506213376, 506213376);

        Polygon poly134604636 = new Polygon(4);
        poly134604636.setPoint(0, new Point(29925, 26720));
        poly134604636.setPoint(1, new Point(29925, 26780));
        poly134604636.setPoint(2, new Point(29675, 26780));
        poly134604636.setPoint(3, new Point(29675, 26720));
        new ShapeRef(router, poly134604636, 134604636);

        Polygon poly14353632 = new Polygon(4);
        poly14353632.setPoint(0, new Point(29600, 26590));
        poly14353632.setPoint(1, new Point(29600, 27660));
        poly14353632.setPoint(2, new Point(28440, 27660));
        poly14353632.setPoint(3, new Point(28440, 26590));
        new ShapeRef(router, poly14353632, 14353632);

        ConnRef connRef70269732 = new ConnRef(router, 70269732);
        ConnEnd srcPt70269732 = new ConnEnd(new Point(29590, 26700), 8);
        connRef70269732.setSourceEndpoint(srcPt70269732);
        ConnEnd dstPt70269732 = new ConnEnd(new Point(29915, 26750), 8);
        connRef70269732.setDestEndpoint(dstPt70269732);
        connRef70269732.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef371572709 = new ConnRef(router, 371572709);
        ConnEnd srcPt371572709 = new ConnEnd(new Point(29590, 26800), 8);
        connRef371572709.setSourceEndpoint(srcPt371572709);
        ConnEnd dstPt371572709 = new ConnEnd(new Point(29915, 26850), 8);
        connRef371572709.setDestEndpoint(dstPt371572709);
        connRef371572709.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef876797766 = new ConnRef(router, 876797766);
        ConnEnd srcPt876797766 = new ConnEnd(new Point(29590, 26900), 8);
        connRef876797766.setSourceEndpoint(srcPt876797766);
        ConnEnd dstPt876797766 = new ConnEnd(new Point(29940, 26950), 8);
        connRef876797766.setDestEndpoint(dstPt876797766);
        connRef876797766.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef134423590 = new ConnRef(router, 134423590);
        ConnEnd srcPt134423590 = new ConnEnd(new Point(29590, 27000), 8);
        connRef134423590.setSourceEndpoint(srcPt134423590);
        ConnEnd dstPt134423590 = new ConnEnd(new Point(29940, 27050), 8);
        connRef134423590.setDestEndpoint(dstPt134423590);
        connRef134423590.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef872586669 = new ConnRef(router, 872586669);
        ConnEnd srcPt872586669 = new ConnEnd(new Point(29590, 27100), 8);
        connRef872586669.setSourceEndpoint(srcPt872586669);
        ConnEnd dstPt872586669 = new ConnEnd(new Point(29940, 27150), 8);
        connRef872586669.setDestEndpoint(dstPt872586669);
        connRef872586669.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef23601612 = new ConnRef(router, 23601612);
        ConnEnd srcPt23601612 = new ConnEnd(new Point(29590, 27200), 8);
        connRef23601612.setSourceEndpoint(srcPt23601612);
        ConnEnd dstPt23601612 = new ConnEnd(new Point(29940, 27250), 8);
        connRef23601612.setDestEndpoint(dstPt23601612);
        connRef23601612.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef737483980 = new ConnRef(router, 737483980);
        ConnEnd srcPt737483980 = new ConnEnd(new Point(29685, 26750), 4);
        connRef737483980.setSourceEndpoint(srcPt737483980);
        ConnEnd dstPt737483980 = new ConnEnd(new Point(29590, 26650), 8);
        connRef737483980.setDestEndpoint(dstPt737483980);
        connRef737483980.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef410784855 = new ConnRef(router, 410784855);
        ConnEnd srcPt410784855 = new ConnEnd(new Point(29685, 26850), 4);
        connRef410784855.setSourceEndpoint(srcPt410784855);
        ConnEnd dstPt410784855 = new ConnEnd(new Point(29590, 26750), 8);
        connRef410784855.setDestEndpoint(dstPt410784855);
        connRef410784855.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef15771480 = new ConnRef(router, 15771480);
        ConnEnd srcPt15771480 = new ConnEnd(new Point(29710, 26950), 4);
        connRef15771480.setSourceEndpoint(srcPt15771480);
        ConnEnd dstPt15771480 = new ConnEnd(new Point(29590, 26850), 8);
        connRef15771480.setDestEndpoint(dstPt15771480);
        connRef15771480.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef34657402 = new ConnRef(router, 34657402);
        ConnEnd srcPt34657402 = new ConnEnd(new Point(29710, 27050), 4);
        connRef34657402.setSourceEndpoint(srcPt34657402);
        ConnEnd dstPt34657402 = new ConnEnd(new Point(29590, 26950), 8);
        connRef34657402.setDestEndpoint(dstPt34657402);
        connRef34657402.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef98191218 = new ConnRef(router, 98191218);
        ConnEnd srcPt98191218 = new ConnEnd(new Point(29710, 27150), 4);
        connRef98191218.setSourceEndpoint(srcPt98191218);
        ConnEnd dstPt98191218 = new ConnEnd(new Point(29590, 27050), 8);
        connRef98191218.setDestEndpoint(dstPt98191218);
        connRef98191218.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef23433311 = new ConnRef(router, 23433311);
        ConnEnd srcPt23433311 = new ConnEnd(new Point(29710, 27250), 4);
        connRef23433311.setSourceEndpoint(srcPt23433311);
        ConnEnd dstPt23433311 = new ConnEnd(new Point(29590, 27150), 8);
        connRef23433311.setDestEndpoint(dstPt23433311);
        connRef23433311.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();
        boolean touching = router.existsOrthogonalTouchingPaths();
        assertFalse(touching);
    }
}