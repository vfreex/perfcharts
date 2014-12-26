package chartgeneration.configtemplate;

import java.util.ArrayList;
import java.util.List;

import chartgeneration.calc.AverageCalculation;
import chartgeneration.common.AddTransformSelector;
import chartgeneration.common.FieldSelector;
import chartgeneration.common.IndexFieldSelector;
import chartgeneration.config.AxisMode;
import chartgeneration.config.Chart2DConfig;
import chartgeneration.config.Chart2DSeriesConfigRule;
import chartgeneration.configtemplate.BaseChart2DTemplateWithInterval;

public class JmeterAverageRTChartTemplate extends BaseChart2DTemplateWithInterval {

	@Override
	public Chart2DConfig generateChartConfig() {
		List<Chart2DSeriesConfigRule> rules;
		FieldSelector timestampField = new IndexFieldSelector(1);
		FieldSelector rtField = new IndexFieldSelector(5);
		FieldSelector xField = new AddTransformSelector(timestampField, rtField);
		rules = new ArrayList<Chart2DSeriesConfigRule>();
		rules.add(new Chart2DSeriesConfigRule("^TX-(.+)-S$", "average", "ms",
				getLabelField(), xField, rtField, new AverageCalculation(
						getInterval())));
		return createConfig("Average Response Times over Time", "time",
				"response time / ms", rules, AxisMode.TIME);
	}

}
