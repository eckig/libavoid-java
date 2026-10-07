package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestCppPortorthordering02
{
    @Test
    public void testorthordering02()
    {
    Router router = new Router(
            Router.RouterFlag.PolyLineRouting.flag | Router.RouterFlag.OrthogonalRouting.flag);
    router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
    router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
    router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 50);
    router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 110);
    router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
    Polygon poly143407352 = new Polygon(4);
    poly143407352.setPoint(0, new Point(6035, 4390));
    poly143407352.setPoint(1, new Point(6035, 5085));
    poly143407352.setPoint(2, new Point(5675, 5085));
    poly143407352.setPoint(3, new Point(5675, 4390));
    new ShapeRef(router, poly143407352, 143407352);
    Polygon poly124950386 = new Polygon(4);
    poly124950386.setPoint(0, new Point(4900, 4090));
    poly124950386.setPoint(1, new Point(4900, 4785));
    poly124950386.setPoint(2, new Point(4540, 4785));
    poly124950386.setPoint(3, new Point(4540, 4090));
    new ShapeRef(router, poly124950386, 124950386);
    ConnRef connRef373967044 = new ConnRef(router, 373967044);
    ConnEnd srcPt373967044 = new ConnEnd(new Point(4890, 4250), 8);
    connRef373967044.setSourceEndpoint(srcPt373967044);
    ConnEnd dstPt373967044 = new ConnEnd(new Point(5685, 4550), 4);
    connRef373967044.setDestEndpoint(dstPt373967044);
    connRef373967044.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef681881486 = new ConnRef(router, 681881486);
    ConnEnd srcPt681881486 = new ConnEnd(new Point(4890, 4325), 8);
    connRef681881486.setSourceEndpoint(srcPt681881486);
    ConnEnd dstPt681881486 = new ConnEnd(new Point(5685, 4625), 4);
    connRef681881486.setDestEndpoint(dstPt681881486);
    connRef681881486.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef829752 = new ConnRef(router, 829752);
    ConnEnd srcPt829752 = new ConnEnd(new Point(4890, 4400), 8);
    connRef829752.setSourceEndpoint(srcPt829752);
    ConnEnd dstPt829752 = new ConnEnd(new Point(5685, 4700), 4);
    connRef829752.setDestEndpoint(dstPt829752);
    connRef829752.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef17625739 = new ConnRef(router, 17625739);
    ConnEnd srcPt17625739 = new ConnEnd(new Point(4890, 4475), 8);
    connRef17625739.setSourceEndpoint(srcPt17625739);
    ConnEnd dstPt17625739 = new ConnEnd(new Point(5685, 4775), 4);
    connRef17625739.setDestEndpoint(dstPt17625739);
    connRef17625739.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef223864175 = new ConnRef(router, 223864175);
    ConnEnd srcPt223864175 = new ConnEnd(new Point(4890, 4550), 8);
    connRef223864175.setSourceEndpoint(srcPt223864175);
    ConnEnd dstPt223864175 = new ConnEnd(new Point(5685, 4850), 4);
    connRef223864175.setDestEndpoint(dstPt223864175);
    connRef223864175.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef98144280 = new ConnRef(router, 98144280);
    ConnEnd srcPt98144280 = new ConnEnd(new Point(4890, 4625), 8);
    connRef98144280.setSourceEndpoint(srcPt98144280);
    ConnEnd dstPt98144280 = new ConnEnd(new Point(5685, 4925), 4);
    connRef98144280.setDestEndpoint(dstPt98144280);
    connRef98144280.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef283100856 = new ConnRef(router, 283100856);
    ConnEnd srcPt283100856 = new ConnEnd(new Point(4890, 4700), 8);
    connRef283100856.setSourceEndpoint(srcPt283100856);
    ConnEnd dstPt283100856 = new ConnEnd(new Point(5685, 5000), 4);
    connRef283100856.setDestEndpoint(dstPt283100856);
    connRef283100856.setRoutingType(ConnType.Orthogonal);
    ConnRef connRef387080925 = new ConnRef(router, 387080925);
    ConnEnd srcPt387080925 = new ConnEnd(new Point(5685, 4475), 4);
    connRef387080925.setSourceEndpoint(srcPt387080925);
    ConnEnd dstPt387080925 = new ConnEnd(new Point(4890, 4175), 8);
    connRef387080925.setDestEndpoint(dstPt387080925);
    connRef387080925.setRoutingType(ConnType.Orthogonal);
    router.processTransaction();
    int crossings = router.existsCrossings();
    assertFalse(crossings > 0);
    }

}
