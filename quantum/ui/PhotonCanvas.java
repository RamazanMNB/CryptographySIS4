package quantum.quantum.ui;

import quantum.quantum.StepData;

import javax.swing.*;
import java.util.Arrays;
import java.awt.*;

/**
 * Custom Swing canvas that animates photon transmission from Alice to Bob.
 * Shows polarization symbols, basis labels, and match/mismatch highlighting.
 */
public class PhotonCanvas extends JPanel {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color BG         = new Color(15,  17,  26);
    private static final Color ALICE_COL  = new Color(86, 182, 254);
    private static final Color BOB_COL    = new Color(80, 200, 120);
    private static final Color EVE_COL    = new Color(255, 100, 100);
    private static final Color PHOTON_COL = new Color(255, 215,   0);
    private static final Color MATCH_COL  = new Color(80,  200, 120);
    private static final Color MISS_COL   = new Color(255, 100, 100);
    private static final Color LANE_COL   = new Color(40,  50,  70);
    private static final Color TEXT_COL   = new Color(200, 210, 230);

    // ── State ─────────────────────────────────────────────────────────────────
    private StepData current    = null;
    private double   animX      = 0.0;  // 0.0 = Alice side, 1.0 = Bob side
    private boolean  animating  = false;
    private boolean  eveEnabled = false;
    private Timer    timer;

    // Recent steps for the trail table
    private final java.util.Deque<StepData> trail = new java.util.ArrayDeque<>();
    private static final int TRAIL_SIZE = 8;

    public PhotonCanvas() {
        setBackground(BG);
        setPreferredSize(new Dimension(720, 320));
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    public void showStep(StepData step, boolean eveEnabled) {
        this.current    = step;
        this.eveEnabled = eveEnabled;
        this.animX      = 0.0;
        this.animating  = true;

        // Add to trail
        trail.addFirst(step);
        if (trail.size() > TRAIL_SIZE) trail.removeLast();

        if (timer != null && timer.isRunning()) timer.stop();
        timer = new Timer(16, e -> {
            animX += 0.035;
            if (animX >= 1.0) {
                animX     = 1.0;
                animating = false;
                timer.stop();
            }
            repaint();
        });
        timer.start();
    }

    public void reset() {
        current   = null;
        animX     = 0;
        animating = false;
        trail.clear();
        if (timer != null) timer.stop();
        repaint();
    }

    // =========================================================================
    // PAINTING
    // =========================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // ── Top half: animation lane ───────────────────────────────────────────
        int laneH = 180;
        drawLane(g2, w, laneH);

        // ── Bottom half: trail table ────────────────────────────────────────────
        drawTrail(g2, w, laneH, h - laneH);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void drawLane(Graphics2D g2, int w, int h) {
        int cy = h / 2;  // center y of lane

        // Background
        g2.setColor(BG);
        g2.fillRect(0, 0, w, h);

        // Channel line
        g2.setColor(LANE_COL);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{8,6}, 0f));
        g2.drawLine(100, cy, w - 100, cy);
        g2.setStroke(new BasicStroke(1.5f));

        // Nodes
        int aliceX = 80, bobX = w - 80;
        int eveX   = w / 2;

        drawNode(g2, aliceX, cy, "Alice", ALICE_COL,
                current != null ? basisLabel(current.aliceBasis, current.alicePol) : "");
        drawNode(g2, bobX,   cy, "Bob",   BOB_COL,
                current != null ? basisLabel(current.bobBasis, current.bobMeasured) : "");

        if (eveEnabled) {
            drawNode(g2, eveX, cy, "Eve", EVE_COL,
                    current != null ? basisLabel(current.eveBasis, current.eveResentPol >= 0 ? current.eveResentPol : 0) : "");
        }

        // Photon
        if (current != null) {
            // Compute photon X based on animation
            double t = eveEnabled ? (animX < 0.5 ? animX * 2 : (animX - 0.5) * 2) : animX;
            int startX = eveEnabled
                    ? (animX < 0.5 ? aliceX + 20 : eveX + 20)
                    : aliceX + 20;
            int endX   = eveEnabled
                    ? (animX < 0.5 ? eveX - 20 : bobX - 20)
                    : bobX - 20;
            int px = (int)(startX + t * (endX - startX));

            // Glow
            for (int r = 18; r >= 6; r -= 3) {
                float alpha = 0.06f * (18 - r);
                g2.setColor(new Color(1f, 0.85f, 0f, alpha));
                g2.fillOval(px - r, cy - r, r * 2, r * 2);
            }
            g2.setColor(PHOTON_COL);
            g2.fillOval(px - 9, cy - 9, 18, 18);

            // Polarization symbol on photon
            drawPolSymbol(g2, px, cy, current.alicePol, Color.BLACK, 12);
        }

        // Match/mismatch badge top-right of lane
        if (current != null && animX >= 1.0) {
            boolean match = current.basesMatch;
            String  label = match ? "Bases matched" : "Bases mismatch";
            Color   col   = match ? MATCH_COL : MISS_COL;
            drawBadge(g2, w - 160, 12, label, col);
        }

        // Phase label top-left
        g2.setColor(new Color(120, 130, 160));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g2.drawString("Photon Transmission", 10, 18);
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void drawTrail(Graphics2D g2, int w, int offsetY, int h) {
        // Background
        g2.setColor(new Color(20, 24, 36));
        g2.fillRect(0, offsetY, w, h);

        // Header separator
        g2.setColor(new Color(50, 60, 80));
        g2.fillRect(0, offsetY, w, 1);

        if (trail.isEmpty()) return;

        int cols = eveEnabled ? 7 : 6;
        String[] headers = eveEnabled
                ? new String[]{"#", "Alice bit", "Alice basis", "Eve basis", "Bob basis", "Match", "Key"}
                : new String[]{"#", "Alice bit", "Alice basis", "Bob basis", "Match", "Key"};

        int[] colW = computeColWidths(w, cols);
        int rowH   = 18;
        int y0     = offsetY + 6;

        // Header row
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(new Color(120, 130, 160));
        drawTableRow(g2, headers, colW, y0, null);

        // Data rows
        int row = 0;
        for (StepData s : trail) {
            int y = y0 + rowH * (row + 1) + 2;
            if (y > offsetY + h - 4) break;
            Color rowBg = (row == 0) ? new Color(40, 50, 70) : null;
            if (rowBg != null) { g2.setColor(rowBg); g2.fillRect(0, y - rowH + 2, w, rowH); }

            String matchStr = s.basesMatch ? "✓" : "✗";
            String keyStr   = s.inSiftedKey ? (s.usedForCheck ? "check" : "keep") : "drop";
            Color  matchCol = s.basesMatch ? MATCH_COL : MISS_COL;

            String[] cells;
            if (eveEnabled) {
                cells = new String[]{
                        String.valueOf(s.index),
                        String.valueOf(s.aliceBit),
                        s.aliceBasis + " " + StepData.polSymbol(s.alicePol),
                        s.eveBasis.equals("-") ? "-" : s.eveBasis,
                        s.bobBasis,
                        matchStr, keyStr
                };
            } else {
                cells = new String[]{
                        String.valueOf(s.index),
                        String.valueOf(s.aliceBit),
                        s.aliceBasis + " " + StepData.polSymbol(s.alicePol),
                        s.bobBasis,
                        matchStr, keyStr
                };
            }

            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            int x = 6;
            for (int c = 0; c < cells.length; c++) {
                Color fc = (c == (eveEnabled ? 5 : 4)) ? matchCol : TEXT_COL;
                g2.setColor(fc);
                g2.drawString(cells[c], x, y);
                x += colW[c];
            }
            row++;
        }
    }

    // =========================================================================
    // DRAW HELPERS
    // =========================================================================

    private void drawNode(Graphics2D g2, int x, int cy, String name, Color col, String sublabel) {
        // Circle
        int r = 22;
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
        g2.fillOval(x - r, cy - r, r * 2, r * 2);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x - r, cy - r, r * 2, r * 2);

        // Name
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(col);
        g2.drawString(name, x - fm.stringWidth(name) / 2, cy - r - 6);

        // Sublabel (basis + pol)
        if (!sublabel.isEmpty()) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
            fm = g2.getFontMetrics();
            g2.setColor(TEXT_COL);
            g2.drawString(sublabel, x - fm.stringWidth(sublabel) / 2, cy + r + 16);
        }
    }

    private void drawPolSymbol(Graphics2D g2, int cx, int cy, int deg, Color col, int size) {
        g2.setColor(col);
        g2.setFont(new Font("SansSerif", Font.BOLD, size));
        String sym = StepData.polSymbol(deg);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(sym, cx - fm.stringWidth(sym) / 2, cy + fm.getAscent() / 2 - 1);
    }

    private void drawBadge(Graphics2D g2, int x, int y, String text, Color col) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int pad = 8;
        int bw = fm.stringWidth(text) + pad * 2;
        int bh = fm.getHeight() + 4;
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 30));
        g2.fillRoundRect(x, y, bw, bh, 8, 8);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, bw, bh, 8, 8);
        g2.drawString(text, x + pad, y + fm.getAscent() + 2);
    }

    private String basisLabel(String basis, int pol) {
        return basis + " " + StepData.polSymbol(pol >= 0 ? pol : 0);
    }

    private void drawTableRow(Graphics2D g2, String[] cells, int[] colW, int y, Color col) {
        int x = 6;
        for (int c = 0; c < cells.length; c++) {
            if (col != null) g2.setColor(col); else g2.setColor(TEXT_COL);
            g2.drawString(cells[c], x, y);
            x += colW[c];
        }
    }

    private int[] computeColWidths(int totalW, int cols) {
        int[] w = new int[cols];
        Arrays.fill(w, totalW / cols);
        return w;
    }

}
