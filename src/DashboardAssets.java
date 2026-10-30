import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.imageio.ImageIO;

/** Optional online images and font resources; the dashboard still works offline. */
final class DashboardAssets {
    private DashboardAssets() {}
    private static Font FIGTREE_BOLD;
    private static final ExecutorService IMAGES = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "player-photo");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, ImageIcon> PHOTO_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, CompletableFuture<BufferedImage>> PHOTO_DOWNLOADS = new ConcurrentHashMap<>();

    static void setCrestIcon(JLabel target,int size){
        target.setToolTipText("Manchester United crest · official club-owned asset");
        String external="https://contentfulproxy.stadion.io/unzgbvss5tuy/2SNJZjFzSOIZuMQyP1zYji/69ab8932dc044c012904e78bdcd057a2/TONAL_CREST_Black%C3%82__3x-png.png?f=center&fm=png&h=160&w=160";
        ImageIcon cached=PHOTO_CACHE.get("crest");
        if(cached!=null){target.setIcon(cached);return;}
        target.setText("◆");target.setFont(uiFont(Font.BOLD,43));target.setForeground(DashboardTheme.RED);
        IMAGES.submit(()->{
            try{
                BufferedImage im=loadImage(external);
                if(im==null)im=loadImage("https://upload.wikimedia.org/wikipedia/en/thumb/7/7a/Manchester_United_FC_crest.svg/200px-Manchester_United_FC_crest.svg.png");
                if(im!=null){ImageIcon resized=scaled(im,size,size);PHOTO_CACHE.put("crest",resized);
                    SwingUtilities.invokeLater(()->{target.setText("");target.setIcon(resized);});}
            }catch(Exception ignored){}
        });
    }
    static void loadPlayerPhoto(String name,JLabel target,int width,int height){
        ImageIcon cached=PHOTO_CACHE.get(name+"#"+width);
        if(cached!=null){target.setText("");target.setIcon(cached);return;}
        CompletableFuture<BufferedImage> future=PHOTO_DOWNLOADS.computeIfAbsent(name,
            n->CompletableFuture.supplyAsync(()->downloadPlayerImage(n),IMAGES));
        future.thenAccept(image->{
            if(image==null)return;
            ImageIcon cropped=crop(image,width,height);
            PHOTO_CACHE.put(name+"#"+width,cropped);
            SwingUtilities.invokeLater(()->{
                if(target.isDisplayable()){
                    target.setText("");target.setIcon(cropped);
                    target.setToolTipText("Photograph from Wikipedia/Wikimedia or TheSportsDB. See original for credits.");
                }
            });
        });
    }
    static BufferedImage downloadPlayerImage(String name){
        try{
            // Optional locally supplied portraits (no photo files redistributed with the ZIP).
            String filename=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("-$","");
            for(String extension:new String[]{"png","jpg","jpeg"}){
                Path file=Path.of("assets","players",filename+"."+extension);
                if(Files.isRegularFile(file))return ImageIO.read(file.toFile());
            }
            String wikiName=name;
            if(wikiName.equals("Benjamin Sesko"))wikiName="Benjamin Šeško";
            if(wikiName.equals("Altay Bayindir"))wikiName="Altay Bayındır";
            if(wikiName.equals("Lisandro Martinez"))wikiName="Lisandro Martínez";
            String api="https://en.wikipedia.org/w/api.php?action=query&redirects=1&format=json&prop=pageimages&piprop=thumbnail&pithumbsize=280&titles="
                +URLEncoder.encode(wikiName,StandardCharsets.UTF_8);
            HttpClient client=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest request=HttpRequest.newBuilder(URI.create(api)).timeout(java.time.Duration.ofSeconds(7))
                .header("User-Agent","Mozilla/5.0 (United Student Dashboard; personal fan project)")
                .header("Accept","application/json").GET().build();
            HttpResponse<String> response=client.send(request,HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()==200){
                Matcher photo=Pattern.compile("\"source\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
                if(photo.find()){
                    String uri=photo.group(1).replace("\\/","/").replace("\\u0026","&");
                    BufferedImage image=loadImage(uri);
                    if(image!=null)return image;
                }
            }
        }catch(Exception ignored){}
        // Secondary optional image source: TheSportsDB player portrait API.
        try{
            String uri="https://www.thesportsdb.com/api/v1/json/3/searchplayers.php?p="+URLEncoder.encode(name,StandardCharsets.UTF_8);
            HttpClient c=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(4)).build();
            HttpRequest req=HttpRequest.newBuilder(URI.create(uri)).timeout(java.time.Duration.ofSeconds(6)).GET().build();
            HttpResponse<String> res=c.send(req,HttpResponse.BodyHandlers.ofString());
            if(res.statusCode()==200){
                Matcher imageUrl=Pattern.compile("\"strCutout\"\\s*:\\s*\"(https?:[^\"]+)\"").matcher(res.body());
                boolean found=imageUrl.find();
                if(!found){imageUrl=Pattern.compile("\"strThumb\"\\s*:\\s*\"(https?:[^\"]+)\"").matcher(res.body());found=imageUrl.find();}
                if(found)return loadImage(imageUrl.group(1).replace("\\/","/"));
            }
        }catch(Exception ignored){}
        return null;
    }
    static BufferedImage loadImage(String url) throws Exception{
        HttpClient client=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest req=HttpRequest.newBuilder(URI.create(url)).timeout(java.time.Duration.ofSeconds(8))
            .header("User-Agent","Mozilla/5.0 UnitedStudentStats/2.0").GET().build();
        HttpResponse<byte[]> resp=client.send(req,HttpResponse.BodyHandlers.ofByteArray());
        if(resp.statusCode()!=200||resp.body().length>7_000_000)return null;
        return ImageIO.read(new ByteArrayInputStream(resp.body()));
    }
    static ImageIcon crop(BufferedImage image,int w,int h){
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        double srcAspect=image.getWidth()/(double)image.getHeight();double dstAspect=w/(double)h;
        int sx=0,sy=0,sw=image.getWidth(),sh=image.getHeight();
        if(srcAspect>dstAspect){sw=(int)Math.round(sh*dstAspect);sx=(image.getWidth()-sw)/2;}
        else{sh=(int)Math.round(sw/dstAspect);sy=(image.getHeight()-sh)/2;}
        g.drawImage(image,0,0,w,h,sx,sy,sx+sw,sy+sh,null);g.dispose();return new ImageIcon(out);
    }
    static ImageIcon scaled(BufferedImage image,int w,int h){
        BufferedImage out=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=out.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(image,0,0,w,h,null);g.dispose();return new ImageIcon(out);
    }
    /**
     * Loads the genuine static Figtree Bold (700) face, not a regular Figtree
     * font with artificially applied bold styling. Font files are never
     * redistributed with this project.
     *
     * Local assets take priority, followed by cached and installed fonts.
     * On first launch, an optional download from the font author's repository
     * makes the correct weight available without bundling binaries.
     */
    static void initialiseFonts() {
        Path cache = Path.of(System.getProperty("user.home"), ".united-dashboard", "Figtree-Bold.otf");
        Path[] candidates = {
            Path.of("fonts", "Figtree-Bold.ttf"),
            Path.of("fonts", "Figtree-Bold.otf"),
            cache,
            Path.of(System.getProperty("user.home"), ".united-dashboard", "Figtree-Bold.ttf")
        };
        for (Path candidate : candidates) {
            if (FIGTREE_BOLD != null) break;
            try {
                InputStream stream = DashboardAssets.class.getResourceAsStream("/fonts/" + candidate.getFileName());
                if (stream == null && Files.isRegularFile(candidate)) stream = Files.newInputStream(candidate);
                if (stream != null) {
                    try (InputStream input = stream) {
                        FIGTREE_BOLD = Font.createFont(Font.TRUETYPE_FONT, input);
                    }
                }
            } catch (Exception ignored) {
                // Try the next candidate; a corrupt local copy must not crash Swing.
            }
        }
        if (FIGTREE_BOLD == null) {
            try {
                for (Font installed : GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()) {
                    String family = installed.getFamily(Locale.ROOT);
                    String face = installed.getFontName(Locale.ROOT);
                    if ("Figtree".equalsIgnoreCase(family) && face.toLowerCase(Locale.ROOT).contains("bold")
                            && !face.toLowerCase(Locale.ROOT).contains("semi")
                            && !face.toLowerCase(Locale.ROOT).contains("extra")) {
                        FIGTREE_BOLD = installed;
                        break;
                    }
                }
            } catch (Exception ignored) {
                // The dashboard also supports machines with no installed Figtree.
            }
        }
        if (FIGTREE_BOLD == null) {
            try {
                String source = "https://raw.githubusercontent.com/erikdkennedy/figtree/master/fonts/otf/Figtree-Bold.otf";
                HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(3))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(source))
                    .timeout(java.time.Duration.ofSeconds(6)).GET().build();
                HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() == 200 && response.body().length > 10000
                        && response.body().length < 2_000_000) {
                    Font downloaded = Font.createFont(Font.TRUETYPE_FONT,
                        new ByteArrayInputStream(response.body()));
                    if (downloaded.getFamily(Locale.ROOT).equalsIgnoreCase("Figtree")) {
                        FIGTREE_BOLD = downloaded;
                        try {
                            Files.createDirectories(cache.getParent());
                            Files.write(cache, response.body());
                        } catch (IOException ignored) {
                            // In-memory Figtree still works for this session.
                        }
                    }
                }
            } catch (Exception ignored) {
                // Offline fallback stays in bold; no fake or placeholder data.
            }
        }
        if (FIGTREE_BOLD == null) {
            System.err.println("Figtree Bold 700 unavailable. Install Figtree Bold or place "
                + "Figtree-Bold.otf / .ttf in the fonts folder. Using a bold sans-serif fallback.");
        }
    }

    /** Apply typography after setting the OS look-and-feel, which resets Swing defaults. */
    static void applySwingDefaults() {
        for (Object key : UIManager.getDefaults().keySet()) {
            if (key.toString().endsWith(".font")) {
                UIManager.put(key, uiFont(Font.BOLD, 14));
            }
        }
    }

    /** All UI text uses the single 700-weight face, including previously PLAIN labels. */
    static Font uiFont(int ignoredStyle, int size) {
        if (FIGTREE_BOLD != null) return FIGTREE_BOLD.deriveFont((float) size);
        return new Font("SansSerif", Font.BOLD, size);
    }
}
