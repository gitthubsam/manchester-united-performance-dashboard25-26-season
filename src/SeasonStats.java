import java.util.*;
import java.util.stream.Collectors;

/** Pure season calculations; independent of Swing so they can be tested quickly. */
final class SeasonStats {
    final int matches, wins, draws, losses, goalsFor, goalsAgainst;

    private SeasonStats(int matches, int wins, int draws, int losses, int goalsFor, int goalsAgainst) {
        this.matches = matches;
        this.wins = wins;
        this.draws = draws;
        this.losses = losses;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
    }

    static SeasonStats from(List<Match> results) {
        int wins = 0, draws = 0, losses = 0, gf = 0, ga = 0;
        for (Match match : results) {
            if (match.result() == 'W') wins++;
            else if (match.result() == 'D') draws++;
            else losses++;
            gf += match.goalsFor;
            ga += match.goalsAgainst;
        }
        return new SeasonStats(results.size(), wins, draws, losses, gf, ga);
    }

    int goalDifference() { return goalsFor - goalsAgainst; }
    long winRatePercent() { return matches == 0 ? 0 : Math.round(100.0 * wins / matches); }

    static String venueWinRate(List<Match> results, String venue) {
        List<Match> filtered = results.stream().filter(m -> m.venue.equals(venue)).collect(Collectors.toList());
        if (filtered.isEmpty()) return "—";
        SeasonStats summary = from(filtered);
        return summary.winRatePercent() + "% (" + summary.wins + "/" + summary.matches + ")";
    }

    static List<Match> filter(List<Match> all, String season, String competition,
                              String venue, String opponentText) {
        String query = opponentText == null ? "" : opponentText.toLowerCase(Locale.ROOT).trim();
        return all.stream().filter(m ->
                (season == null || season.equals("All seasons") || m.season.equals(season)) &&
                (competition == null || competition.equals("All competitions") || m.competition.equals(competition)) &&
                (venue == null || venue.equals("All venues") || m.venue.equals(venue)) &&
                m.opponent.toLowerCase(Locale.ROOT).contains(query))
            .sorted(Comparator.comparing((Match m) -> m.date)).collect(Collectors.toList());
    }
}
