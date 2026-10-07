package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

public class TestCppPortjunction
{
    @Test
    public void testjunction01()
    {
    Router router = new Router(Router.RouterFlag.OrthogonalRouting);
    router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
    router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 105);
    router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
    Rectangle rect478845150 = new Rectangle(new Point(50695, 51070), new Point(50705, 51080));
    ShapeRef shapeRef478845150 = new ShapeRef(router, rect478845150, 478845150);
    ShapeConnectionPin pin = new ShapeConnectionPin(shapeRef478845150, 
	    ShapeConnectionPin.CONNECTIONPIN_CENTRE, ShapeConnectionPin.ATTACH_POS_CENTRE, 
	    ShapeConnectionPin.ATTACH_POS_CENTRE, true, 0.0, ConnDirFlag.ConnDirNone);
    pin.setExclusive(false);
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
    ConnEnd dstPt352019675 = new ConnEnd(shapeRef478845150, ShapeConnectionPin.CONNECTIONPIN_CENTRE);
    new ConnRef(router, srcPt352019675, dstPt352019675, 352019675);
    ConnEnd srcPt42699400 = new ConnEnd(shapeRef478845150, ShapeConnectionPin.CONNECTIONPIN_CENTRE);
    ConnEnd dstPt42699400 = new ConnEnd(new Point(50615, 51075), 8);
    new ConnRef(router, srcPt42699400, dstPt42699400, 42699400);
    ConnEnd srcPt94712625 = new ConnEnd(new Point(50710, 50550), 4);
    ConnEnd dstPt94712625 = new ConnEnd(shapeRef478845150, ShapeConnectionPin.CONNECTIONPIN_CENTRE);
    new ConnRef(router, srcPt94712625, dstPt94712625, 94712625);
    ConnEnd srcPt92802970 = new ConnEnd(new Point(50990, 50450), 8);
    ConnEnd dstPt92802970 = new ConnEnd(new Point(51275, 50485), 1);
    new ConnRef(router, srcPt92802970, dstPt92802970, 92802970);
    ConnEnd srcPt716502036 = new ConnEnd(new Point(50710, 50500), 4);
    ConnEnd dstPt716502036 = new ConnEnd(new Point(51800, 50925), 15);
    new ConnRef(router, srcPt716502036, dstPt716502036, 716502036);
    router.processTransaction();
    }

    @Test
    public void testjunction02()
    {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 105);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
        JunctionRef junction478845150 = new JunctionRef(router, new Point(50700, 51075));
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
        ConnEnd dstPt352019675 = new ConnEnd(junction478845150);
        new ConnRef(router, srcPt352019675, dstPt352019675, 352019675);
        ConnEnd srcPt42699400 = new ConnEnd(junction478845150);
        ConnEnd dstPt42699400 = new ConnEnd(new Point(50615, 51075), 8);
        new ConnRef(router, srcPt42699400, dstPt42699400, 42699400);
        ConnEnd srcPt94712625 = new ConnEnd(new Point(50710, 50550), 4);
        ConnEnd dstPt94712625 = new ConnEnd(junction478845150);
        new ConnRef(router, srcPt94712625, dstPt94712625, 94712625);
        ConnEnd srcPt92802970 = new ConnEnd(new Point(50990, 50450), 8);
        ConnEnd dstPt92802970 = new ConnEnd(new Point(51275, 50485), 1);
        new ConnRef(router, srcPt92802970, dstPt92802970, 92802970);
        ConnEnd srcPt716502036 = new ConnEnd(new Point(50710, 50500), 4);
        ConnEnd dstPt716502036 = new ConnEnd(new Point(51800, 50925), 15);
        new ConnRef(router, srcPt716502036, dstPt716502036, 716502036);
        router.processTransaction();
        router.moveJunction(junction478845150, 585, 0);
        router.processTransaction();
    }

    @Test
    public void testjunction03()
    {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 105);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
        Rectangle rect478845150 = new Rectangle(new Point(50695, 51070), new Point(50705, 51080));
        JunctionRef junctionRef478845150 = new JunctionRef(router,
                new Point(50700, 51075), 478845150);
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
        ConnEnd dstPt352019675 = new ConnEnd(junctionRef478845150);
        new ConnRef(router, srcPt352019675, dstPt352019675, 352019675);
        ConnEnd srcPt42699400 = new ConnEnd(junctionRef478845150);
        ConnEnd dstPt42699400 = new ConnEnd(new Point(50615, 51075), 8);
        new ConnRef(router, srcPt42699400, dstPt42699400, 42699400);
        ConnEnd srcPt94712625 = new ConnEnd(new Point(50710, 50550), 4);
        ConnEnd dstPt94712625 = new ConnEnd(junctionRef478845150);
        new ConnRef(router, srcPt94712625, dstPt94712625, 94712625);
        ConnEnd srcPt92802970 = new ConnEnd(new Point(50990, 50450), 8);
        ConnEnd dstPt92802970 = new ConnEnd(new Point(51275, 50485), 1);
        new ConnRef(router, srcPt92802970, dstPt92802970, 92802970);
        ConnEnd srcPt716502036 = new ConnEnd(new Point(50710, 50500), 4);
        ConnEnd dstPt716502036 = new ConnEnd(new Point(51800, 50925), 15);
        new ConnRef(router, srcPt716502036, dstPt716502036, 716502036);
        router.processTransaction();
        router.moveJunction(junctionRef478845150, 585, 0);
        router.processTransaction();
    }

    @Test
    public void testjunction04()
    {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        Rectangle shapeRect1 = new Rectangle(new Point(0, 0), new Point(30, 20));
        ShapeRef shapeRef1 = new ShapeRef(router, shapeRect1);
        Rectangle shapeRect2 = new Rectangle(new Point(70, 7), new Point(100, 27));
        new ShapeRef(router, shapeRect2);
        Rectangle shapeRect3 = new Rectangle(new Point(50, 60), new Point(80, 155));
        new ShapeRef(router, shapeRect3);
        Rectangle shapeRect4 = new Rectangle(new Point(125, 60), new Point(155, 80));
        new ShapeRef(router, shapeRect4);
        Rectangle shapeRect5 = new Rectangle(new Point(15, 150), new Point(45, 170));
        ShapeRef shapeRef5 = new ShapeRef(router, shapeRect5);
        Rectangle shapeRect6 = new Rectangle(new Point(130, 130), new Point(160, 150));
        ShapeRef shapeRef6 = new ShapeRef(router, shapeRect6);
        new ShapeConnectionPin(shapeRef1, ShapeConnectionPin.CONNECTIONPIN_CENTRE,
                ShapeConnectionPin.ATTACH_POS_CENTRE, ShapeConnectionPin.ATTACH_POS_CENTRE, true, 0.0, ConnDirFlag.ConnDirNone);
        new ShapeConnectionPin(shapeRef5, ShapeConnectionPin.CONNECTIONPIN_CENTRE,
                ShapeConnectionPin.ATTACH_POS_CENTRE, ShapeConnectionPin.ATTACH_POS_CENTRE, true, 0.0, ConnDirFlag.ConnDirNone);
        new ShapeConnectionPin(shapeRef6, ShapeConnectionPin.CONNECTIONPIN_CENTRE,
                ShapeConnectionPin.ATTACH_POS_CENTRE, ShapeConnectionPin.ATTACH_POS_CENTRE, true, 0.0, ConnDirFlag.ConnDirNone);
        ConnEnd srcEnd = new ConnEnd(shapeRef1, ShapeConnectionPin.CONNECTIONPIN_CENTRE);
        ConnEnd dstEnd = new ConnEnd(shapeRef6, ShapeConnectionPin.CONNECTIONPIN_CENTRE);
        ConnRef conn1= new ConnRef(router, srcEnd, dstEnd);
        router.processTransaction();
        Pair<JunctionRef, ConnRef> newObjs =
                conn1.splitAtSegment(2);
        router.processTransaction();
        ConnEnd srcEnd3 = new ConnEnd(shapeRef5, ShapeConnectionPin.CONNECTIONPIN_CENTRE);
        ConnEnd dstEnd3 = new ConnEnd(newObjs.first);
        new ConnRef(router, srcEnd3, dstEnd3);
        router.processTransaction();
        router.deleteConnector(conn1);
        conn1 = null;
        router.processTransaction();
        newObjs.first.removeJunctionAndMergeConnectors();
        router.processTransaction();
    }

}
