package io.github.eckig.libavoid;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TestNudgeIntoBug
{

    @Test
    public void testNudge()
    {
        Router router = new Router(Router.RouterFlag.OrthogonalRouting);
        router.setRoutingPenalty(Router.RoutingParameter.segmentPenalty, 50);
        router.setRoutingPenalty(Router.RoutingParameter.anglePenalty, 0);
        router.setRoutingPenalty(Router.RoutingParameter.crossingPenalty, 200);
        router.setRoutingPenalty(Router.RoutingParameter.fixedSharedPathPenalty, 110);
        router.setRoutingParameter(Router.RoutingParameter.idealNudgingDistance, 25);
        Rectangle rect548374163 = new Rectangle(new Point(51900, 50400), new Point(52300, 50900));
        new ShapeRef(router, rect548374163, 548374163);
        Rectangle rect901116812 = new Rectangle(new Point(51131, 49750), new Point(51531, 50100));
        new ShapeRef(router, rect901116812, 901116812);
        Rectangle rect335855988 = new Rectangle(new Point(51125, 50175), new Point(51525, 50900));
        new ShapeRef(router, rect335855988, 335855988);
        Rectangle rect448725420 = new Rectangle(new Point(52375, 50750), new Point(52575, 50900));
        new ShapeRef(router, rect448725420, 448725420);
        Rectangle rect74263573 = new Rectangle(new Point(51900, 49750), new Point(52300, 50100));
        new ShapeRef(router, rect74263573, 74263573);
        ConnEnd srcPt463223880 = new ConnEnd(new Point(51500, 50275), 8);
        ConnEnd dstPt463223880 = new ConnEnd(new Point(51150, 50275), 4);
        ConnEnd srcPt144520410 = new ConnEnd(new Point(51150, 49850), 4);
        ConnEnd dstPt144520410 = new ConnEnd(new Point(51500, 50350), 8);
        ConnEnd srcPt45398340 = new ConnEnd(new Point(52400, 50825), 4);
        ConnEnd dstPt45398340 = new ConnEnd(new Point(51500, 49925), 8);
        ConnEnd srcPt29344262 = new ConnEnd(new Point(51150, 50500), 4);
        ConnEnd dstPt29344262 = new ConnEnd(new Point(51925, 50000), 4);
        new ConnRef(router, srcPt29344262, dstPt29344262, 29344262);
        new ConnRef(router, srcPt45398340, dstPt45398340, 45398340);
        new ConnRef(router, srcPt144520410, dstPt144520410, 144520410);
        new ConnRef(router, srcPt463223880, dstPt463223880, 463223880);
        router.processTransaction();
        boolean overlap = router.existsOrthogonalFixedSegmentOverlap();
        boolean touching = router.existsOrthogonalTouchingPaths();
        assertFalse(overlap);
        assertFalse(touching);
    }
}
