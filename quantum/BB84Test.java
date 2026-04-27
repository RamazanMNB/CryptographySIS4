package quantum.quantum;

import quantum.quantum.CascadeCorrection;
import quantum.quantum.E91Protocol;

import java.util.*;

/**
 * Testing & Validation suite for BB84 and E91 protocols.
 * Run with: java -cp out quantum.quantum.BB84Test
 * Or:       java -cp out quantum.Main --test
 */
public class BB84Test {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════╗");
        System.out.println("║   BB84 + E91 Protocol Test Suite          ║");
        System.out.println("╚═══════════════════════════════════════════╝");

        section("4.1 Quantum Mechanics Validation");
        testSameBasisDeterministic();
        testDifferentBasisRandom();
        testBasisMatchRate();

        section("4.2 Protocol Correctness");
        testNoEavesdropperSuccess();
        testWithEavesdropperAborts();
        testKeyAgreement();

        section("4.3 Statistical Tests");
        testKeyEfficiency();
        testScalability();
        testMultipleRunsAverage();
        testEdgeCaseMinPhotons();

        section("4.4 Security Analysis");
        testDetectionProbability();
        testEveErrorRate();

        section("Cascade Error Correction (Bonus)");
        testCascadeReducesErrors();
        testCascadeWithNoErrors();

        section("E91 Protocol Tests (Bonus)");
        testE91NoEveSuccess();
        testE91WithEveDetected();
        testE91BellInequality();
        testE91KeyAgreement();

        summary();
    }

    static void testSameBasisDeterministic() {
        int mismatches = 0, trials = 10_000;
        for (int i = 0; i < trials; i++) {
            int bit = (int)(Math.random() * 2);
            Photon.Basis basis = Math.random() < 0.5 ? Photon.Basis.RECTILINEAR : Photon.Basis.DIAGONAL;
            if (new Photon(bit, basis).measure(basis) != bit) mismatches++;
        }
        assertEquals("Same basis -> 100% accuracy", 0, mismatches);
    }

    static void testDifferentBasisRandom() {
        int matches = 0, trials = 10_000;
        for (int i = 0; i < trials; i++) {
            int bit = (int)(Math.random() * 2);
            if (new Photon(bit, Photon.Basis.RECTILINEAR).measure(Photon.Basis.DIAGONAL) == bit)
                matches++;
        }
        assertInRange("Different basis -> ~50% random", (double) matches / trials, 0.45, 0.55);
    }

    static void testBasisMatchRate() {
        BB84Result r = new BB84Protocol(new SimulationConfig(10_000, false)).run();
        assertInRange("Basis match rate ~50%", r.basisMatchRate, 0.45, 0.55);
    }

    static void testNoEavesdropperSuccess() {
        BB84Result r = new BB84Protocol(new SimulationConfig(1000, false)).run();
        assertFalse("No Eve -> not aborted", r.aborted);
        assertInRange("No Eve -> error rate <= 5%", r.errorRate, 0.0, 0.05);
    }

    static void testWithEavesdropperAborts() {
        int aborts = 0;
        for (int i = 0; i < 20; i++)
            if (new BB84Protocol(new SimulationConfig(1000, true)).run().aborted) aborts++;
        assertTrue("Eve present -> aborts >=18/20", aborts >= 18);
    }

    static void testKeyAgreement() {
        BB84Result r = null;
        for (int i = 0; i < 20; i++) {
            r = new BB84Protocol(new SimulationConfig(1000, false)).run();
            if (!r.aborted && r.finalKeyLength > 0) break;
        }
        if (r == null || r.aborted) { fail("Key Agreement - no successful run"); return; }
        assertEquals("Same final key length", r.aliceFinalKey.size(), r.bobFinalKey.size());
        assertTrue("Alice == Bob final key", r.aliceFinalKey.equals(r.bobFinalKey));
    }

    static void testKeyEfficiency() {
        BB84Result r = new BB84Protocol(new SimulationConfig(2000, false)).run();
        if (!r.aborted) assertInRange("Key efficiency 10%-45%", r.keyEfficiency / 100.0, 0.10, 0.45);
        else skip("Key Efficiency - aborted");
    }

    static void testScalability() {
        for (int n : new int[]{100, 1000, 10_000}) {
            BB84Result r = new BB84Protocol(new SimulationConfig(n, false)).run();
            assertFalse("Scalability n=" + n + " succeeds", r.aborted);
        }
    }

    static void testMultipleRunsAverage() {
        int runs = 100; double bmSum = 0, erSum = 0; int ok = 0;
        for (int i = 0; i < runs; i++) {
            BB84Result r = new BB84Protocol(new SimulationConfig(500, false)).run();
            bmSum += r.basisMatchRate; erSum += r.errorRate;
            if (!r.aborted) ok++;
        }
        assertInRange("Avg basis match ~50% (100 runs)", bmSum / runs, 0.45, 0.55);
        assertInRange("Avg error rate <=5% (100 runs)",  erSum / runs, 0.0,  0.05);
        assertTrue(">=95% runs succeed without Eve", ok >= 95);
    }

    static void testEdgeCaseMinPhotons() {
        try {
            new BB84Protocol(new SimulationConfig(10, false)).run();
            assertTrue("10 photons - no crash", true);
        } catch (Exception e) { fail("10 photons threw: " + e.getMessage()); }
    }

    static void testDetectionProbability() {
        int detected = 0;
        for (int i = 0; i < 50; i++)
            if (new BB84Protocol(new SimulationConfig(1000, true)).run().aborted) detected++;
        assertInRange("Detection prob >95%", (double) detected / 50, 0.95, 1.0);
    }

    static void testEveErrorRate() {
        double sum = 0; int n = 30;
        for (int i = 0; i < n; i++)
            sum += new BB84Protocol(new SimulationConfig(2000, true)).run().errorRate;
        assertInRange("Eve error rate ~25% (0.20-0.30)", sum / n, 0.20, 0.30);
    }

    static void testCascadeReducesErrors() {
        int len = 500;
        List<Integer> alice = new ArrayList<>(), bob = new ArrayList<>();
        Random rng = new Random(42);
        for (int i = 0; i < len; i++) {
            int bit = rng.nextInt(2);
            alice.add(bit);
            bob.add(rng.nextDouble() < 0.05 ? 1 - bit : bit);
        }
        int before = countErrors(alice, bob);
        List<Integer> corrected = new CascadeCorrection(alice, bob).correct();
        int after = countErrors(alice, corrected);
        assertTrue("Cascade reduces errors (" + before + " -> " + after + ")", after <= before);
    }

    static void testCascadeWithNoErrors() {
        List<Integer> key = new ArrayList<>();
        Random rng = new Random();
        for (int i = 0; i < 200; i++) key.add(rng.nextInt(2));
        List<Integer> copy = new ArrayList<>(key);
        List<Integer> corrected = new CascadeCorrection(key, copy).correct();
        assertEquals("Cascade no errors -> same length", key.size(), corrected.size());
        assertTrue("Cascade no errors -> identical", key.equals(corrected));
    }

    static void testE91NoEveSuccess() {
        E91Protocol.E91Result r = new E91Protocol(2000, false).run();
        assertFalse("E91 no Eve -> not aborted", r.aborted);
        assertTrue("E91 Bell test passed", r.bellTestPassed);
        assertTrue("E91 CHSH |S| > 2.0", Math.abs(r.chshValue) > 2.0);
    }

    static void testE91WithEveDetected() {
        int aborts = 0;
        for (int i = 0; i < 20; i++)
            if (new E91Protocol(2000, true).run().aborted) aborts++;
        assertTrue("E91 Eve -> aborts >=18/20", aborts >= 18);
    }

    static void testE91BellInequality() {
        double sum = 0; int n = 20;
        for (int i = 0; i < n; i++)
            sum += Math.abs(new E91Protocol(5000, false).run().chshValue);
        assertInRange("E91 CHSH |S| in (2.0, 2*sqrt(2)+0.1)", sum / n, 2.0, 2.0 * Math.sqrt(2) + 0.1);
    }

    static void testE91KeyAgreement() {
        E91Protocol.E91Result r = new E91Protocol(2000, false).run();
        if (r.aborted) { skip("E91 Key Agreement - aborted"); return; }
        assertTrue("E91 key not empty", r.aliceFinalKey.size() > 0);
        assertEquals("E91 same key length", r.aliceFinalKey.size(), r.bobFinalKey.size());
        assertTrue("E91 Alice == Bob key", r.aliceFinalKey.equals(r.bobFinalKey));
    }

    private static int countErrors(List<Integer> a, List<Integer> b) {
        int c = 0;
        for (int i = 0; i < Math.min(a.size(), b.size()); i++)
            if (!a.get(i).equals(b.get(i))) c++;
        return c;
    }

    static void assertEquals(String n, int a, int b) { if (a==b) pass(n); else fail(n+"["+a+"!="+b+"]"); }
    static void assertEquals(String n, Object a, Object b) { if (Objects.equals(a,b)) pass(n); else fail(n); }
    static void assertTrue(String n, boolean c)  { if (c) pass(n); else fail(n); }
    static void assertFalse(String n, boolean c) { assertTrue(n, !c); }
    static void assertInRange(String n, double v, double lo, double hi) {
        if (v >= lo && v <= hi) pass(n);
        else fail(n + String.format(" [%.4f not in [%.4f,%.4f]]", v, lo, hi));
    }
    static void section(String t) { System.out.println("\n-- " + t + " " + "-".repeat(Math.max(0,44-t.length()))); }
    static void pass(String n)    { System.out.println("  + " + n); passed++; }
    static void fail(String n)    { System.out.println("  FAIL: " + n); failed++; }
    static void skip(String n)    { System.out.println("  o SKIP: " + n); }
    static void summary() {
        System.out.println("\n╔═══════════════════════════════════════════╗");
        System.out.printf( "║  Results: %2d passed, %2d failed            ║%n", passed, failed);
        System.out.println("╚═══════════════════════════════════════════╝");
        if (failed > 0) System.exit(1);
    }
}
