package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

public class TestCppPortnudgeold
{
    @Test
    public void testnudgeold()
    {
    Router router = new Router(
            Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
    router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
    router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 110);
    router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
    Polygon poly426345871 = new Polygon(4);
    poly426345871.setPoint(0, new Point(7200, 5240));
    poly426345871.setPoint(1, new Point(7200, 5660));
    poly426345871.setPoint(2, new Point(6690, 5660));
    poly426345871.setPoint(3, new Point(6690, 5240));
    new ShapeRef(router, poly426345871, 426345871);
    Polygon poly596896212 = new Polygon(4);
    poly596896212.setPoint(0, new Point(7200, 5740));
    poly596896212.setPoint(1, new Point(7200, 6160));
    poly596896212.setPoint(2, new Point(6690, 6160));
    poly596896212.setPoint(3, new Point(6690, 5740));
    new ShapeRef(router, poly596896212, 596896212);
    ConnRef connRef20026701 = new ConnRef(router, 20026701);
    ConnEnd srcPt20026701 = new ConnEnd(new Point(7190, 5600), 8);
    connRef20026701.setSourceEndpoint(srcPt20026701);
    ConnEnd dstPt20026701 = new ConnEnd(new Point(7190, 5800), 8);
    connRef20026701.setDestEndpoint(dstPt20026701);
    connRef20026701.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef124776000 = new ConnRef(router, 124776000);
    ConnEnd srcPt124776000 = new ConnEnd(new Point(7190, 5550), 8);
    connRef124776000.setSourceEndpoint(srcPt124776000);
    ConnEnd dstPt124776000 = new ConnEnd(new Point(7190, 5850), 8);
    connRef124776000.setDestEndpoint(dstPt124776000);
    connRef124776000.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef162425002 = new ConnRef(router, 162425002);
    ConnEnd srcPt162425002 = new ConnEnd(new Point(7190, 5500), 8);
    connRef162425002.setSourceEndpoint(srcPt162425002);
    ConnEnd dstPt162425002 = new ConnEnd(new Point(7190, 5900), 8);
    connRef162425002.setDestEndpoint(dstPt162425002);
    connRef162425002.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef445102866 = new ConnRef(router, 445102866);
    ConnEnd srcPt445102866 = new ConnEnd(new Point(7190, 5450), 8);
    connRef445102866.setSourceEndpoint(srcPt445102866);
    ConnEnd dstPt445102866 = new ConnEnd(new Point(7190, 5950), 8);
    connRef445102866.setDestEndpoint(dstPt445102866);
    connRef445102866.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef472520508 = new ConnRef(router, 472520508);
    ConnEnd srcPt472520508 = new ConnEnd(new Point(7190, 5400), 8);
    connRef472520508.setSourceEndpoint(srcPt472520508);
    ConnEnd dstPt472520508 = new ConnEnd(new Point(7190, 6000), 8);
    connRef472520508.setDestEndpoint(dstPt472520508);
    connRef472520508.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef406587600 = new ConnRef(router, 406587600);
    ConnEnd srcPt406587600 = new ConnEnd(new Point(7190, 5350), 8);
    connRef406587600.setSourceEndpoint(srcPt406587600);
    ConnEnd dstPt406587600 = new ConnEnd(new Point(7190, 6050), 8);
    connRef406587600.setDestEndpoint(dstPt406587600);
    connRef406587600.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef41657796 = new ConnRef(router, 41657796);
    ConnEnd srcPt41657796 = new ConnEnd(new Point(7190, 5300), 8);
    connRef41657796.setSourceEndpoint(srcPt41657796);
    ConnEnd dstPt41657796 = new ConnEnd(new Point(7190, 6100), 8);
    connRef41657796.setDestEndpoint(dstPt41657796);
    connRef41657796.setRoutingType(ConnType.Orthogonal);
    router.processTransaction();
    }

}
