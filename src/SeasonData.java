import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.Collectors;

/** CSV parsing and access to the read-only, attributed 2025/26 dataset. */
final class SeasonData {
    static final String SEASON = "2025/26";
    private SeasonData() {}

    static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }

    static List<Match> parseMatches(List<String> lines)throws IOException{
        List<Match> data=new ArrayList<>();if(lines.isEmpty())throw new IOException("The CSV is empty.");
        Map<String,Integer> h=header(lines.get(0));
        boolean footballData=h.containsKey("hometeam")&&h.containsKey("awayteam")&&h.containsKey("fthg")&&h.containsKey("ftag");
        if(!footballData && !(h.containsKey("date")&&h.containsKey("opponent")&&h.containsKey("goalsfor")&&h.containsKey("goalsagainst")))
            throw new IOException("Unknown CSV columns. See the README for supported formats.");
        for(int i=1;i<lines.size();i++){
            String line=lines.get(i);if(line.isBlank())continue;
            List<String> a=parseCsvLine(line);
            try{
                LocalDate date=parseDate(field(a,h,"date"));
                if(footballData){
                    String home=field(a,h,"hometeam"),away=field(a,h,"awayteam");
                    boolean isHome=isUnited(home),isAway=isUnited(away);
                    if(!isHome&&!isAway)continue;
                    String opp=isHome?away:home;
                    String venue=isHome?"Home":"Away";
                    String season=seasonFromDate(date);
                    int gf=Integer.parseInt(field(a,h,isHome?"fthg":"ftag"));
                    int ga=Integer.parseInt(field(a,h,isHome?"ftag":"fthg"));
                    data.add(new Match(date,season,"Premier League",opp,venue,gf,ga));
                }else{
                    String season=fieldDefault(a,h,"season",seasonFromDate(date));
                    String comp=fieldDefault(a,h,"competition","Premier League");
                    String venue=fieldDefault(a,h,"venue","Home");
                    if(!venue.equalsIgnoreCase("Home")&&!venue.equalsIgnoreCase("Away"))throw new IllegalArgumentException("Venue must be Home or Away");
                    data.add(new Match(date,season,comp,field(a,h,"opponent"),
                        venue.equalsIgnoreCase("Home")?"Home":"Away",
                        Integer.parseInt(field(a,h,"goalsfor")),Integer.parseInt(field(a,h,"goalsagainst"))));
                }
            }catch(Exception e){throw new IOException("CSV row "+(i+1)+": "+e.getMessage(),e);}
        }
        for (Match match : data) {
            if (!seasonFromDate(match.date).equals(match.season)) {
                throw new IOException("Date and season do not match for " + match.opponent + " on " + match.date);
            }
        }
        return data;
    }
    static List<Player> parsePlayers(List<String> lines)throws IOException{
        List<Player> data=new ArrayList<>();if(lines.isEmpty())throw new IOException("Empty file.");
        Map<String,Integer> h=header(lines.get(0));
        for(String required:new String[]{"name","goals","assists"})if(!h.containsKey(required))
            throw new IOException("Missing column: "+required+". Expected Name,Position,Appearances,Goals,Assists,Season");
        for(int i=1;i<lines.size();i++){
            if(lines.get(i).isBlank())continue;
            List<String> a=parseCsvLine(lines.get(i));
            try{
                data.add(new Player(fieldDefault(a,h,"season",SEASON),field(a,h,"name"),
                    fieldDefault(a,h,"position","Player"),
                    Integer.parseInt(fieldDefault(a,h,"appearances", "0")),
                    Integer.parseInt(fieldDefault(a,h,"starts","-1")),
                    Integer.parseInt(fieldDefault(a,h,"minutes","-1")),
                    Integer.parseInt(field(a,h,"goals")),Integer.parseInt(field(a,h,"assists")),
                    Double.parseDouble(fieldDefault(a,h,"xg","NaN")),
                    Double.parseDouble(fieldDefault(a,h,"xa","NaN"))));
            }catch(Exception e){throw new IOException("Player CSV row "+(i+1)+": "+e.getMessage(),e);}
        }
        return data;
    }
    static Map<String,Integer> header(String line){
        List<String> names=parseCsvLine(line.replace("\ufeff",""));Map<String,Integer> map=new HashMap<>();
        for(int i=0;i<names.size();i++)map.put(names.get(i).replaceAll("[^A-Za-z0-9]","").toLowerCase(Locale.ROOT),i);
        return map;
    }
    static String field(List<String> values,Map<String,Integer> keys,String key){
        Integer index=keys.get(key);if(index==null||index>=values.size())throw new IllegalArgumentException("Missing field: "+key);
        String val=values.get(index).trim();if(val.isEmpty())throw new IllegalArgumentException("Blank field: "+key);
        return val;
    }
    static String fieldDefault(List<String> values,Map<String,Integer> keys,String key,String fallback){
        Integer index=keys.get(key);
        if(index==null||index>=values.size()||values.get(index).isBlank())return fallback;
        return values.get(index).trim();
    }
    static List<String> parseCsvLine(String line){
        List<String> values=new ArrayList<>();StringBuilder b=new StringBuilder();boolean quoted=false;
        for(int i=0;i<line.length();i++){
            char ch=line.charAt(i);
            if(ch=='"'){
                if(quoted&&i+1<line.length()&&line.charAt(i+1)=='"'){b.append('"');i++;}
                else quoted=!quoted;
            }else if(ch==','&&!quoted){values.add(b.toString().trim());b.setLength(0);}
            else b.append(ch);
        }
        if (quoted) throw new IllegalArgumentException("Unclosed quoted field");
        values.add(b.toString().trim());return values;
    }
    static LocalDate parseDate(String text){
        DateTimeFormatter[] formats={DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu"),DateTimeFormatter.ofPattern("dd/MM/uu"),
            DateTimeFormatter.ofPattern("dd.MM.uuuu"),DateTimeFormatter.ofPattern("dd-MM-uuuu")};
        for(DateTimeFormatter f:formats)try{return LocalDate.parse(text,f);}catch(DateTimeParseException ignored){}
        throw new IllegalArgumentException("Unrecognised date '"+text+"' (use YYYY-MM-DD)");
    }
    static String seasonFromDate(LocalDate d){
        int year=d.getMonthValue()>=7?d.getYear():d.getYear()-1;
        return year+"/"+String.format("%02d",(year+1)%100);
    }
    static boolean isUnited(String name){
        String n=name.trim().toLowerCase(Locale.ROOT).replace(".","");
        return n.equals("man united")||n.equals("man utd")||n.equals("manchester united")||n.equals("manchester utd");
    }

    static void requireSeason(List<String> seasons) throws IOException {
        if(seasons.isEmpty())throw new IOException("No 2025/26 records in CSV.");
        for(String season:seasons)if(!SEASON.equals(season))
            throw new IOException("This dashboard only accepts 2025/26 data. Found: "+season);
    }
    /**
     * Finds a 2025/26 CSV regardless of where IntelliJ's working directory is set.
     *
     * The distributable JAR embeds CSVs as /data resources. IntelliJ, however,
     * normally compiles only src/*.java and may start Java from a directory that
     * doesn't contain data/. When running unpackaged classes, search next to the
     * class output folder and its parents to find the extracted project directory.
     */
    static List<String> dataset(String file) throws IOException {
        try (InputStream in = SeasonData.class.getResourceAsStream("/data/" + file)) {
            if (in != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    return reader.lines().collect(Collectors.toList());
                }
            }
        }

        LinkedHashSet<Path> possibleFiles = new LinkedHashSet<>();
        addCandidatePaths(possibleFiles, Path.of("").toAbsolutePath(), file);
        try {
            var codeSource = SeasonData.class.getProtectionDomain().getCodeSource();
            if (codeSource != null && codeSource.getLocation() != null
                    && "file".equalsIgnoreCase(codeSource.getLocation().getProtocol())) {
                Path classesLocation = Path.of(codeSource.getLocation().toURI()).toAbsolutePath();
                Path startingDirectory = Files.isDirectory(classesLocation)
                        ? classesLocation : classesLocation.getParent();
                if (startingDirectory != null) {
                    addCandidatePaths(possibleFiles, startingDirectory, file);
                }
            }
        } catch (java.net.URISyntaxException | SecurityException ignored) {
            // The project working directory is still a valid fallback.
        }

        for (Path candidate : possibleFiles) {
            if (Files.isRegularFile(candidate)) {
                return Files.readAllLines(candidate, StandardCharsets.UTF_8);
            }
        }
        throw new IOException("Can't find data/" + file
                + ". Extract the COMPLETE ZIP, then open its project folder in IntelliJ "
                + "(the folder with src/ and data/), not an individual Java file.");
    }

    private static void addCandidatePaths(Set<Path> locations, Path directory, String file) {
        // Covers IntelliJ's out/production/<module>, Java's out/ and build/classes/ paths.
        for (int depth = 0; directory != null && depth < 8;
                directory = directory.getParent(), depth++) {
            locations.add(directory.resolve("data").resolve(file));
        }
    }
    static List<Match> loadMatches(){
        try { List<Match> result=parseMatches(dataset("matches-2025-26.csv"));
            requireSeason(result.stream().map(m->m.season).collect(Collectors.toList()));return result;
        }catch(IOException e){throw new IllegalStateException("Required verified matches missing: "+e.getMessage(),e);}
    }
    static List<Player> loadPlayers(){
        try { List<Player> result=parsePlayers(dataset("players-2025-26.csv"));
            requireSeason(result.stream().map(m->m.season).collect(Collectors.toList()));return result;
        }catch(IOException e){throw new IllegalStateException("Required verified players missing: "+e.getMessage(),e);}
    }
}
