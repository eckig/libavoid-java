package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestRestrictedNudging
{
    @Test
    public void testNudging()
    {
        Router router = new Router(Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 200);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 110);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);

        Polygon poly282634758=new Polygon(4);
        poly282634758.setPoint(0, new Point(51360, 50215));
        poly282634758.setPoint(1, new Point(51360, 50460));
        poly282634758.setPoint(2, new Point(51075, 50460));
        poly282634758.setPoint(3, new Point(51075, 50215));
        new ShapeRef(router, poly282634758, 282634758);

        Polygon poly69386928=new Polygon(4);
        poly69386928.setPoint(0, new Point(51050, 50415));
        poly69386928.setPoint(1, new Point(51050, 50735));
        poly69386928.setPoint(2, new Point(50690, 50735));
        poly69386928.setPoint(3, new Point(50690, 50415));
        new ShapeRef(router, poly69386928, 69386928);

        Polygon poly11958280=new Polygon(4);
        poly11958280.setPoint(0, new Point(50460, 50490));
        poly11958280.setPoint(1, new Point(50460, 50660));
        poly11958280.setPoint(2, new Point(50290, 50660));
        poly11958280.setPoint(3, new Point(50290, 50490));
        new ShapeRef(router, poly11958280, 11958280);

        Polygon poly50591298=new Polygon(4);
        poly50591298.setPoint(0, new Point(51260, 50015));
        poly50591298.setPoint(1, new Point(51260, 50185));
        poly50591298.setPoint(2, new Point(51075, 50185));
        poly50591298.setPoint(3, new Point(51075, 50015));
        new ShapeRef(router, poly50591298, 50591298);

        ConnRef connRef355676284 = new ConnRef(router, 355676284);
        ConnEnd srcPt355676284=new ConnEnd(new Point(51040, 50575), 8);
        connRef355676284.setSourceEndpoint(srcPt355676284);
        ConnEnd dstPt355676284=new ConnEnd(new Point(51085, 50300), 4);
        connRef355676284.setDestEndpoint(dstPt355676284);
        connRef355676284.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef33653259 = new ConnRef(router, 33653259);
        ConnEnd srcPt33653259=new ConnEnd(new Point(51040, 50650), 8);
        connRef33653259.setSourceEndpoint(srcPt33653259);
        ConnEnd dstPt33653259=new ConnEnd(new Point(51085, 50375), 4);
        connRef33653259.setDestEndpoint(dstPt33653259);
        connRef33653259.setRoutingType(ConnType.Orthogonal);

        ConnRef connRef421608980 = new ConnRef(router, 421608980);
        ConnEnd srcPt421608980=new ConnEnd(new Point(51040, 50500), 8);
        connRef421608980.setSourceEndpoint(srcPt421608980);
        ConnEnd dstPt421608980=new ConnEnd(new Point(51085, 50100), 4);
        connRef421608980.setDestEndpoint(dstPt421608980);
        connRef421608980.setRoutingType(ConnType.Orthogonal);

        router.processTransaction();

        boolean overlap = router.existsOrthogonalTouchingPaths();
        assertFalse(overlap);
    }
}
