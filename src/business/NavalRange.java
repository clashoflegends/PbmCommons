package business;

import business.converter.ConverterFactory;
import business.facade.LocalFacade;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.Set;
import java.util.HashSet;
import model.Local;

/** Stateful naval reach for maps containing Landing access. Costs are actual paid costs. */
public final class NavalRange {
    private record State(Local hex, int previous, boolean terminal, boolean landingPath) { }
    private record Node(State state, int cost, ArmyPath path, long sequence) { }
    private NavalRange() { }

    public static HashMap<Local, ArmyPath> calculate(SortedMap<String, Local> map,
            MovimentoExercito settings, Set<Local> landingHexes) {
        return search(map, settings, landingHexes,
                Map.of(settings.getOrigem(), landingHexes.contains(settings.getOrigem())), false, new HashSet<>());
    }

    public record Forecast(Map<Local, ArmyPath> firstTurn, Map<Local, ArmyPath> secondTurn) { }

    /** Two bounded searches, with a fresh order at each valid first-turn endpoint. */
    public static Forecast forecast(SortedMap<String, Local> map, MovimentoExercito settings,
            Set<Local> landingHexes) {
        Set<Local> landingArrivals = new HashSet<>();
        var first = search(map, settings, landingHexes,
                Map.of(settings.getOrigem(), landingHexes.contains(settings.getOrigem())), true, landingArrivals);
        Map<Local, Boolean> starts = new HashMap<>();
        starts.put(settings.getOrigem(), landingHexes.contains(settings.getOrigem()));
        first.forEach((hex, path) -> starts.put(hex, landingArrivals.contains(hex)));
        return new Forecast(first, search(map, settings, landingHexes, starts, false, new HashSet<>()));
    }

    public static boolean touchesLanding(ArmyPath path, Set<Local> landingHexes) {
        return landingHexes.contains(path.getStart()) || path.getSteps().stream()
                .anyMatch(step -> landingHexes.contains(step.getDestino()));
    }

    private static HashMap<Local, ArmyPath> search(SortedMap<String, Local> map,
            MovimentoExercito settings, Set<Local> landingHexes,
            Map<Local, Boolean> starts, boolean includeUnaffected, Set<Local> landingArrivals) {
        HashMap<Local, ArmyPath> result = new HashMap<>();
        LocalFacade facade = new LocalFacade();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(Node::cost)
                .thenComparingLong(Node::sequence));
        Map<State, Integer> best = new HashMap<>();
        long sequence = 0;
        for (var seed : starts.entrySet()) {
            State start = new State(seed.getKey(), 0, false, seed.getValue());
            queue.add(new Node(start, 0, new ArmyPath(seed.getKey()), sequence++));
            best.put(start, 0);
        }
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            if (best.get(node.state()) != node.cost() || node.state().terminal()) continue;
            for (int direction = 1; direction <= 6; direction++) {
                Local next = map.get(facade.getIdentificacaoVizinho(node.state().hex(), direction));
                if (next == null || next == node.path().getStart()) continue;
                MovimentoExercito step;
                ArmyPath path;
                try {
                    step = settings.clone();
                } catch (CloneNotSupportedException e) { throw new IllegalStateException(e); }
                step.setOrigem(node.state().hex()); step.setDestino(next);
                step.setDirecao(direction); step.setDirecaoAnterior(node.state().previous());
                int cost = step.getCustoMovimento();
                if (cost <= 0 || cost > 99 || cost > settings.getLimiteMovimento() - node.cost()) continue;
                int total = node.cost() + cost;
                State state = new State(next, ConverterFactory.getDirecao(direction + 3), step.isLandingStop(),
                        node.state().landingPath() || landingHexes.contains(next));
                if (best.getOrDefault(state, Integer.MAX_VALUE) <= total) continue;
                try { path = node.path().clone(); }
                catch (CloneNotSupportedException e) { throw new IllegalStateException(e); }
                best.put(state, total); path.setCost(total); path.addStep(step);
                if (state.landingPath()) landingArrivals.add(next);
                if ((includeUnaffected || state.landingPath())
                        && (!result.containsKey(next) || result.get(next).getCost() > total)) {
                    result.put(next, path);
                }
                queue.add(new Node(state, total, path, sequence++));
            }
        }
        return result;
    }

    /** Non-Shore naval Landing sites; decorative tags on water/native anchorages are ignored. */
    public static Set<Local> landingHexes(SortedMap<String, Local> map) {
        Set<Local> result = new HashSet<>();
        LocalFacade facade = new LocalFacade();
        for (Local to : map.values()) {
            if (!NavalAccess.hasLanding(to) || to.getTerreno().isLitoral()
                    || facade.isAncoravelSemLanding(to)) continue;
            for (int side = 1; side <= 6; side++) {
                if (!to.isLanding(side)) continue;
                Local from = map.get(facade.getIdentificacaoVizinho(to, side));
                if (from != null && from.getTerreno() != null && from.getTerreno().isAgua()) {
                    result.add(to);
                    break;
                }
            }
        }
        return result;
    }
}
