import java.util.Objects;

/** Season totals from a named statistical provider, not a player profile guess. */
final class Player {
    final String name, position, season;
    final int appearances, starts, minutes, goals, assists;
    final double xg, xa;

    Player(String season, String name, String position, int appearances, int goals, int assists) {
        this(season, name, position, appearances, -1, -1, goals, assists, Double.NaN, Double.NaN);
    }

    Player(String season, String name, String position, int appearances, int starts,
           int minutes, int goals, int assists, double xg, double xa) {
        this.season = Objects.requireNonNull(season);
        this.name = Objects.requireNonNull(name);
        this.position = Objects.requireNonNull(position);
        if (appearances < 0 || goals < 0 || assists < 0 || starts < -1 || minutes < -1)
            throw new IllegalArgumentException("Player counts cannot be negative");
        this.appearances = appearances;
        this.starts = starts;
        this.minutes = minutes;
        this.goals = goals;
        this.assists = assists;
        this.xg = xg;
        this.xa = xa;
    }
}
