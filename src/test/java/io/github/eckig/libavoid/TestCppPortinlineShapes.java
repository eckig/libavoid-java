package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

public class TestCppPortinlineShapes
{
    @Test
    public void testinlineShapes()
    {
    Router router = new Router(Router.RouterFlag.OrthogonalRouting);
    router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
    router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 0);
    Polygon poly1 = new Polygon(4);
    poly1.setPoint(0, new Point(52, 188));
    poly1.setPoint(1, new Point(52, 292));
    poly1.setPoint(2, new Point(-52, 292));
    poly1.setPoint(3, new Point(-52, 188));
    ShapeRef shapeRef1 = new ShapeRef(router, poly1, 1);
    new ShapeConnectionPin(shapeRef1, 544, 0.5, 0.5, true, 10, 0);
    Polygon poly2 = new Polygon(4);
    poly2.setPoint(0, new Point(52, -52));
    poly2.setPoint(1, new Point(52, 52));
    poly2.setPoint(2, new Point(-52, 52));
    poly2.setPoint(3, new Point(-52, -52));
    ShapeRef shapeRef2 = new ShapeRef(router, poly2, 2);
    new ShapeConnectionPin(shapeRef2, 544, 0.5, 0.5, true, 10, 0);
    Polygon poly3 = new Polygon(4);
    poly3.setPoint(0, new Point(52, 428));
    poly3.setPoint(1, new Point(52, 532));
    poly3.setPoint(2, new Point(-52, 532));
    poly3.setPoint(3, new Point(-52, 428));
    ShapeRef shapeRef3 = new ShapeRef(router, poly3, 3);
    new ShapeConnectionPin(shapeRef3, 544, 0.5, 0.5, true, 10, 0);
    Polygon poly4 = new Polygon(4);
    poly4.setPoint(0, new Point(52, 68));
    poly4.setPoint(1, new Point(52, 172));
    poly4.setPoint(2, new Point(-52, 172));
    poly4.setPoint(3, new Point(-52, 68));
    ShapeRef shapeRef4 = new ShapeRef(router, poly4, 4);
    new ShapeConnectionPin(shapeRef4, 544, 0.5, 0.5, true, 10, 0);
    Polygon poly5 = new Polygon(4);
    poly5.setPoint(0, new Point(52, 308));
    poly5.setPoint(1, new Point(52, 412));
    poly5.setPoint(2, new Point(-52, 412));
    poly5.setPoint(3, new Point(-52, 308));
    ShapeRef shapeRef5 = new ShapeRef(router, poly5, 5);
    new ShapeConnectionPin(shapeRef5, 544, 0.5, 0.5, true, 10, 0);
    ConnRef connRef6 = new ConnRef(router, 6);
    ConnEnd srcPt6 = new ConnEnd(shapeRef2, 544);
    connRef6.setSourceEndpoint(srcPt6);
    ConnEnd dstPt6 = new ConnEnd(shapeRef5, 544);
    connRef6.setDestEndpoint(dstPt6);
    connRef6.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef7 = new ConnRef(router, 7);
    ConnEnd srcPt7 = new ConnEnd(shapeRef5, 544);
    connRef7.setSourceEndpoint(srcPt7);
    ConnEnd dstPt7 = new ConnEnd(shapeRef4, 544);
    connRef7.setDestEndpoint(dstPt7);
    connRef7.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef8 = new ConnRef(router, 8);
    ConnEnd srcPt8 = new ConnEnd(shapeRef1, 544);
    connRef8.setSourceEndpoint(srcPt8);
    ConnEnd dstPt8 = new ConnEnd(shapeRef5, 544);
    connRef8.setDestEndpoint(dstPt8);
    connRef8.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef9 = new ConnRef(router, 9);
    ConnEnd srcPt9 = new ConnEnd(shapeRef2, 544);
    connRef9.setSourceEndpoint(srcPt9);
    ConnEnd dstPt9 = new ConnEnd(shapeRef3, 544);
    connRef9.setDestEndpoint(dstPt9);
    connRef9.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef10 = new ConnRef(router, 10);
    ConnEnd srcPt10 = new ConnEnd(shapeRef3, 544);
    connRef10.setSourceEndpoint(srcPt10);
    ConnEnd dstPt10 = new ConnEnd(shapeRef4, 544);
    connRef10.setDestEndpoint(dstPt10);
    connRef10.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef11 = new ConnRef(router, 11);
    ConnEnd srcPt11 = new ConnEnd(shapeRef1, 544);
    connRef11.setSourceEndpoint(srcPt11);
    ConnEnd dstPt11 = new ConnEnd(shapeRef3, 544);
    connRef11.setDestEndpoint(dstPt11);
    connRef11.setRoutingType(ConnType.Orthogonal);
    router.processTransaction();
    }
}
