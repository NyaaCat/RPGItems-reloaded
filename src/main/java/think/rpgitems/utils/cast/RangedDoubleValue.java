package think.rpgitems.utils.cast;

import static think.rpgitems.power.Utils.weightedRandomPick;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import think.rpgitems.utils.WeightedPair;

public class RangedDoubleValue {
  private List<WeightedPair<Double, Double>> ranges = new ArrayList<>();

  public int getSplitSize() {
    return ranges.size();
  }

  Double totalLength = null;

  public double getTotalLength() {
    if (totalLength == null) {
      synchronized (this) {
        if (totalLength == null) {
          totalLength = ranges.stream().mapToDouble(pair -> length(pair)).sum();
        }
      }
    }
    return totalLength;
  }

  private double length(WeightedPair<Double, Double> pair) {
    return Math.abs(pair.getValue() - pair.getKey());
  }

  public double random() {
    WeightedPair<Double, Double> pair = weightedRandomPick(ranges);
    if (pair.getKey().equals(pair.getValue())) {
      return pair.getKey();
    }
    double range = pair.getValue() - pair.getKey();
    double selected = ThreadLocalRandom.current().nextDouble() * range + pair.getKey();
    return selected;
  }

  public static RangedDoubleValue of(String s) {
    RangedDoubleValue value = new RangedDoubleValue();
    String[] split = s.split(" ");
    for (String s1 : split) {
      value.ranges.add(parse(s1));
    }
    return value;
  }

  private static WeightedPair<Double, Double> parse(String s1) {
    String s = s1;
    int weight;
    double lower, upper;
    if (s1.contains(":")) {
      String[] split = s1.split(":");
      weight = Integer.parseInt(split[1]);
      s = split[0];
    } else {
      weight = 1;
    }
    if (s.contains(",")) {
      String[] split = s.split(",");
      double a1 = Double.parseDouble(split[0]);
      double a2 = Double.parseDouble(split[1]);
      lower = Math.min(a1, a2);
      upper = Math.max(a1, a2);
    } else {
      lower = upper = Double.parseDouble(s);
    }
    return new WeightedPair<>(lower, upper, weight);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    List<WeightedPair<Double, Double>> weightedPairs = this.ranges;
    for (int i = 0; i < weightedPairs.size(); i++) {
      WeightedPair<Double, Double> weightedPair = weightedPairs.get(i);
      Double key = weightedPair.getKey();
      Double value = weightedPair.getValue();
      int weight = weightedPair.getWeight();
      if (key.equals(value)) {
        sb.append(String.format("%.4f", value));
      } else {
        sb.append(String.format("%.4f,%.4f", key, value));
      }
      if (weight != 1) {
        sb.append(String.format(":%d", weight));
      }
      if (i != weightedPairs.size()) {
        sb.append(" ");
      }
    }
    return sb.toString();
  }

  public double uniformed(double i, int amount) {
    if (getSplitSize() == 0) {
      return 0;
    }
    double totalLength = getTotalLength();
    double selected = (totalLength / amount) * i;
    return select(selected);
  }

  private double select(double selected) {
    double totalLength = getTotalLength();
    if (selected != totalLength) {
      selected = selected % totalLength;
    }

    double remain = selected;
    double result = selected;
    for (int i = 0; i < getSplitSize(); i++) {
      WeightedPair<Double, Double> pair = ranges.get(i);
      double length = length(pair);
      if (remain >= 0 && remain <= length) {
        result = pair.getKey() + remain;
        break;
      }
      remain -= length;
    }
    return result;
  }
}
