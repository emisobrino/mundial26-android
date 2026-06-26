import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Genera la capa de frente del adaptive icon (el "26" + franja tricolor) en PNG
 * para todas las densidades. El fondo del icono es un vector sólido #2A2C2C
 * (sin blanco). La franja queda dentro de la zona segura para no recortarse.
 */
public class IconGen {
    static final Color DARK = new Color(0x2A, 0x2C, 0x2C);
    static final Color GREEN = new Color(0x3C, 0xAC, 0x3B);
    static final Color RED = new Color(0xE6, 0x1D, 0x25);
    static final Color BLUE = new Color(0x2A, 0x39, 0x8D);

    public static void main(String[] args) throws Exception {
        String base = args[0]; // app/src/main/res
        // adaptive foreground = 108dp; zona segura = centro 66dp
        int[] sizes = {108, 162, 216, 324, 432};
        String[] dpis = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
        for (int i = 0; i < sizes.length; i++) {
            BufferedImage img = renderForeground(sizes[i]);
            File dir = new File(base, "mipmap-" + dpis[i]);
            dir.mkdirs();
            ImageIO.write(img, "png", new File(dir, "ic_launcher_foreground.png"));
        }
        // Play Store icon (512x512) con fondo, para referencia
        BufferedImage store = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setup(store);
        g.setColor(DARK);
        g.fill(new RoundRectangle2D.Float(0, 0, 512, 512, 120, 120));
        drawBadge(g, 512, 1.0);
        g.dispose();
        new File(base, "../../../tools").mkdirs();
        ImageIO.write(store, "png", new File(args[0] + "/../../../../tools/playstore-512.png"));
        System.out.println("OK iconos generados");
    }

    static BufferedImage renderForeground(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setup(img);
        // El contenido se dibuja dentro de la zona segura (centro 66/108 = 0.611)
        drawBadge(g, size, 66.0 / 108.0);
        g.dispose();
        return img;
    }

    static Graphics2D setup(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g;
    }

    /** Dibuja "26" + franja tricolor centrados, escalados a 'scale' del lienzo. */
    static void drawBadge(Graphics2D g, int size, double scale) {
        double cx = size / 2.0, cy = size / 2.0;
        double content = size * scale;

        // Tipografía "26" italic + bold
        Font font = pickFont((int) (content * 0.62));
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String txt = "26";
        int tw = fm.stringWidth(txt);
        int ascent = fm.getAscent(), descent = fm.getDescent();
        double textH = ascent + descent;

        // Reservamos una franja tricolor debajo del texto
        double stripeH = content * 0.085;
        double stripeW = content * 0.62;
        double gap = content * 0.06;
        double blockH = textH + gap + stripeH;
        double top = cy - blockH / 2.0;

        // "26" blanco
        g.setColor(Color.WHITE);
        float baseline = (float) (top + ascent);
        g.drawString(txt, (float) (cx - tw / 2.0), baseline);

        // Franja tricolor (3 segmentos iguales), redondeada
        double sy = top + textH + gap;
        double sx = cx - stripeW / 2.0;
        double seg = stripeW / 3.0;
        double r = stripeH / 2.0;
        Color[] cols = {GREEN, RED, BLUE};
        for (int i = 0; i < 3; i++) {
            g.setColor(cols[i]);
            // esquinas redondeadas solo en extremos; usamos rects con leve radio
            double rr = (i == 0 || i == 2) ? r : 0;
            g.fill(new RoundRectangle2D.Double(sx + i * seg, sy, seg + (i < 2 ? 1 : 0), stripeH, rr, rr));
        }
    }

    static Font pickFont(int sizePx) {
        String[] prefs = {"Arial Black", "Segoe UI Black", "Segoe UI", "Arial", Font.SANS_SERIF};
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String p : prefs) {
            for (String a : available) {
                if (a.equalsIgnoreCase(p)) {
                    return new Font(a, Font.BOLD | Font.ITALIC, sizePx);
                }
            }
        }
        return new Font(Font.SANS_SERIF, Font.BOLD | Font.ITALIC, sizePx);
    }
}
