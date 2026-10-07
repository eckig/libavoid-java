package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

public class TestCppPorttjunct
{
    @Test
    public void testtjunct()
    {
    Router router = new Router(Router.RouterFlag.OrthogonalRouting);
    router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
    router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 200);
    router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 105);
    router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
    JunctionRef jRef1 = new JunctionRef(router, new Point(50700, 51075));
    Rectangle rect92712048 = new Rectangle(new Point(51246, 50475), new Point(51304, 50585));
    new ShapeRef(router, rect92712048, 92712048);
    Rectangle rect23127921 = new Rectangle(new Point(50365, 50766), new Point(50485, 50834));
    new ShapeRef(router, rect23127921, 23127921);
    Rectangle rect159957148 = new Rectangle(new Point(51740, 50891), new Point(51860, 50959));
    new ShapeRef(router, rect159957148, 159957148);
    Rectangle rect1350214 = new Rectangle(new Point(50375, 51015), new Point(50625, 51135));
    new ShapeRef(router, rect1350214, 1350214);
    Rectangle rect247197258 = new Rectangle(new Point(50265, 51046), new Point(50375, 51104));
    new ShapeRef(router, rect247197258, 247197258);
    Rectangle rect281096654 = new Rectangle(new Point(50700, 50390), new Point(51000, 50610));
    new ShapeRef(router, rect281096654, 281096654);
    ConnEnd srcPt342420237 = new ConnEnd(new Point(50425, 50800), 15);
    ConnEnd dstPt342420237 = new ConnEnd(new Point(50710, 50450), 4);
    new ConnRef(router, srcPt342420237, dstPt342420237, 342420237);
    ConnEnd srcPt352019675 = new ConnEnd(new Point(50990, 50500), 8);
    ConnEnd dstPt352019675 = new ConnEnd(jRef1);
    new ConnRef(router, srcPt352019675, dstPt352019675, 352019675);
    ConnEnd srcPt42699400 = new ConnEnd(jRef1);
    ConnEnd dstPt42699400 = new ConnEnd(new Point(50615, 51075), 8);
    new ConnRef(router, srcPt42699400, dstPt42699400, 42699400);
    ConnEnd srcPt94712625 = new ConnEnd(new Point(50710, 50550), 4);
    ConnEnd dstPt94712625 = new ConnEnd(jRef1);
    new ConnRef(router, srcPt94712625, dstPt94712625, 94712625);
    ConnEnd srcPt92802970 = new ConnEnd(new Point(50990, 50450), 8);
    ConnEnd dstPt92802970 = new ConnEnd(new Point(51275, 50485), 1);
    new ConnRef(router, srcPt92802970, dstPt92802970, 92802970);
    ConnEnd srcPt716502036 = new ConnEnd(new Point(50710, 50500), 4);
    ConnEnd dstPt716502036 = new ConnEnd(new Point(51800, 50925), 15);
    new ConnRef(router, srcPt716502036, dstPt716502036, 716502036);
    router.processTransaction();
    }

}
