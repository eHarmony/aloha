package com.eharmony.matching.featureSpecExtractor.vw

import java.text.DecimalFormat
import java.util.regex.Pattern

class VwFeatureNormalizer extends (CharSequence => String) with java.io.Serializable {
    private[this] val lineRegex = Pattern.compile("\\|(\\w)\\s+([^\\|]+)")
    private[this] val namespaceRegex = ".+:(.+)".r
    private[this] val format = new DecimalFormat("0.00000")

    def apply(vwLine: CharSequence): String = {
        val matcher = lineRegex.matcher(vwLine)
        val sb = new StringBuffer
        while(matcher.find) {
            matcher.appendReplacement(sb, "|" + matcher.group(1) + ":" + format.format(normalizeNamespace(matcher.group(2))) + " " + matcher.group(2))
        }
        matcher.appendTail(sb)
        sb.toString
    }

    private[this] def normalizeNamespace(namespace: String): Double = {
        var sum = 0d
        namespace.split("\\s+").foreach {
            case namespaceRegex(w) =>
                val currentWeight = w.toDouble
                sum += currentWeight * currentWeight
            case _ => sum += 1
        }
        if (sum == 0) 0
        else 1.0 / math.sqrt(sum)
    }
}

object VwFeatureNormalizer {
    val instance = new VwFeatureNormalizer
}

/*
public class VWFeatureNormalizer implements Function<String, String>, Serializable {
    private final Pattern lineRegex;
    private final Pattern namespaceRegex;
    private final DecimalFormat format;

    public VWFeatureNormalizer() {
        this.lineRegex = Pattern.compile("\\|(\\w)\\s+([^\\|]+)");
        this.namespaceRegex = Pattern.compile(".+:(.+)");
        this.format = new DecimalFormat("0.00000");
    }

    @Override
    public String apply(String vwLine) {
        Matcher matcher = lineRegex.matcher(vwLine);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "|" + matcher.group(1) + ":" + format.format(normalizeNamespace(matcher.group(2))) + " " + matcher.group(2));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private double normalizeNamespace(String namespace) {
        double sum = 0;
        for (String s : namespace.split("\\s+")) {
            Matcher matcher = namespaceRegex.matcher(s);
            double currentWeight;
            if (matcher.matches()) {
                currentWeight = Double.parseDouble(matcher.group(1));
            }
            else {
                currentWeight = 1;
            }
            sum += currentWeight * currentWeight;
        }
        return 1.0 / Math.sqrt(sum);
    }
}

*/

