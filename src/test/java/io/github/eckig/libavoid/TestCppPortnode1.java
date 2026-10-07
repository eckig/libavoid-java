package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

public class TestCppPortnode1
{
    @Test
    public void testnode1()
    {
    Router router = new Router(
            Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
    router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
    router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 200);
    router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
    double buffer = 4;
    Polygon poly342721632 = new Polygon(4);
    poly342721632.setPoint(0, new Point(51910, 50946));
    poly342721632.setPoint(1, new Point(51910, 51004));
    poly342721632.setPoint(2, new Point(51825, 51004));
    poly342721632.setPoint(3, new Point(51825, 50946));
    ShapeRef shapeRef342721632 = new ShapeRef(router, poly342721632, 342721632);
    new ShapeConnectionPin(shapeRef342721632, 1, 
            ShapeConnectionPin.ATTACH_POS_LEFT, ShapeConnectionPin.ATTACH_POS_CENTRE, true, buffer, ConnDirFlag.ConnDirLeft);
    Polygon poly807558175 = new Polygon(4);
    poly807558175.setPoint(0, new Point(51604, 51000));
    poly807558175.setPoint(1, new Point(51604, 51085));
    poly807558175.setPoint(2, new Point(51546, 51085));
    poly807558175.setPoint(3, new Point(51546, 51000));
    ShapeRef shapeRef807558175 = new ShapeRef(router, poly807558175, 807558175);
    new ShapeConnectionPin(shapeRef807558175, 1, 
	    ShapeConnectionPin.ATTACH_POS_CENTRE, ShapeConnectionPin.ATTACH_POS_TOP, true, buffer, ConnDirFlag.ConnDirNone);
    Polygon poly404900496 = new Polygon(4);
    poly404900496.setPoint(0, new Point(51525, 50946));
    poly404900496.setPoint(1, new Point(51525, 51004));
    poly404900496.setPoint(2, new Point(51440, 51004));
    poly404900496.setPoint(3, new Point(51440, 50946));
    ShapeRef shapeRef404900496 = new ShapeRef(router, poly404900496, 404900496);
    new ShapeConnectionPin(shapeRef404900496, 1, 
	    ShapeConnectionPin.ATTACH_POS_RIGHT, ShapeConnectionPin.ATTACH_POS_CENTRE, true, buffer, 8);
    JunctionRef junctionRef265539540 = new JunctionRef(router, new Point(51625, 50950), 265539540);
    ConnRef connRef985644192 = new ConnRef(router, 985644192);
    ConnEnd srcPt985644192 = new ConnEnd(shapeRef404900496, 1);
    connRef985644192.setSourceEndpoint(srcPt985644192);
    ConnEnd dstPt985644192 = new ConnEnd(junctionRef265539540);
    connRef985644192.setDestEndpoint(dstPt985644192);
    connRef985644192.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef467016913 = new ConnRef(router, 467016913);
    ConnEnd srcPt467016913 = new ConnEnd(junctionRef265539540);
    connRef467016913.setSourceEndpoint(srcPt467016913);
    ConnEnd dstPt467016913 = new ConnEnd(shapeRef807558175, 1);
    connRef467016913.setDestEndpoint(dstPt467016913);
    connRef467016913.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef45740440 = new ConnRef(router, 45740440);
    ConnEnd srcPt45740440 = new ConnEnd(shapeRef342721632, 1);
    connRef45740440.setSourceEndpoint(srcPt45740440);
    ConnEnd dstPt45740440 = new ConnEnd(junctionRef265539540);
    connRef45740440.setDestEndpoint(dstPt45740440);
    connRef45740440.setRoutingType(ConnType.Orthogonal);
    router.processTransaction();
    }

}
