package perfcharts.generator;

import perfcharts.chart.GenericTable;
import perfcharts.chart.TableCell;
import perfcharts.common.FieldSelector;
import perfcharts.common.IndexFieldSelector;
import perfcharts.config.PerformanceSimpleComparisonTableConfig;
import perfcharts.model.DataTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by vfreex on 1/5/16.
 */
public class PerformanceSimpleComparisonTableGenerator implements ChartGenerator {
    private final static Logger LOGGER = Logger
            .getLogger(PerformanceComparisonTableGenerator.class.getName());
    private final static Pattern txPattern = Pattern.compile("TX-(.+)");
    private PerformanceSimpleComparisonTableConfig config;

    public PerformanceSimpleComparisonTableGenerator(
            PerformanceSimpleComparisonTableConfig config) {
        this.config = config;
    }

    @Override
    public GenericTable generate(DataTable dataTable) throws Exception {
        LOGGER.info("Generating Perf Comparison Table '" + config.getTitle()
                + "' (" + config.getKey() + ")...");
        GenericTable table = new GenericTable(config.createChartFactory()
                .createFormatter());
        table.setTitle(config.getTitle());
        table.setSubtitle(config.getSubtitle());
        table.setKey("perfcharts-simple-perfcmp");
        final int COLUMNS = 8;
        table.setHeader(new String[]{
                "Transaction",
                "#Samples",
                "#Samples diff", "Average", "Average diff", "Average diff%",
/*                "90% line", "90% line diff", "90% line diff%",*/
/*                "Throughput (tx/h)", "Throughput diff", "Throughput diff%",*/
                "Error%", "Error% diff"});
        table.setHeaderTooltip(new String[]{
                "a transaction is a collection of end-user actions that represent typical application activity",
                "number of repeated executions of this build",
                "sample difference between this build and the baseline build",
                "average response time of this build, in seconds, where response time is the total amount of time it takes to respond to a request for service",
                "difference of average response time between this build and the baseline build, in seconds, where a positive value means performance degradation and a negative value means performance improvement",
                "percentage change of average response time between this build and the baseline build, where a positive value means performance degradation and a negative value means performance improvement",
                "percentage of failed transactions, calculated by the number of failed requests / the number of all requests",
                "percentage difference of failed transactions between this build and the baseline build, where a positive value means error% is increased and a a negative value means error% is decreased",
        });
        table.setColumnWidths(new String[]{
                null, "10%", "10%", "10%", "10%", "10%", "10%", "10%"
        });
        table.setSortList(new int[][]{
                new int[]{4, 0},
                new int[]{5, 0}
        });

        FieldSelector labelField = new IndexFieldSelector(0);
        FieldSelector sSampleField = new IndexFieldSelector(1);
        FieldSelector dSampleField = new IndexFieldSelector(2);
        FieldSelector sAverageField = new IndexFieldSelector(3);
        FieldSelector dAverageField = new IndexFieldSelector(4);
//        FieldSelector s90LineField = new IndexFieldSelector(5);
//        FieldSelector d90LineField = new IndexFieldSelector(6);
        FieldSelector sErrorField = new IndexFieldSelector(7);
        FieldSelector dErrorField = new IndexFieldSelector(8);
        //FieldSelector sThroughputField = new IndexFieldSelector(9);
        //FieldSelector dThroughputField = new IndexFieldSelector(10);
        TableCell[] totalRow = null;// = new TableCell[columns];
        Map<String, TableCell[]> tx2RowMap = new TreeMap<String, TableCell[]>();
        for (List<Object> dataRow : dataTable.getRows()) {
            String label = labelField.select(dataRow).toString();
            TableCell[] row = null;
            if ("TOTAL".equals(label)) {
                row = totalRow = new TableCell[COLUMNS];
                row[0] = new TableCell("TOTAL");
            } else {
                Matcher m = txPattern.matcher(label);
                if (m.matches()) {
                    String txName = m.group(1);
                    row = new TableCell[COLUMNS];
                    row[0] = new TableCell(txName);
                    tx2RowMap.put(txName, row);
                } else {
                    continue;
                }
            }
            long sSample = (long) sSampleField.select(dataRow);
            long dSample = (long) dSampleField.select(dataRow);
            row[1] = new TableCell(sSample);
            row[2] = new TableCell(sSample - dSample);
            double sAverage = (double) sAverageField.select(dataRow);
            double dAverage = (double) dAverageField.select(dataRow);
            double diffAverage = sAverage - dAverage;
            row[3] = new TableCell(sAverage);
            row[3].setCssClass("perfcharts-unit-ms2s");
            row[4] = new TableCell(diffAverage);
            if (Double.isFinite(diffAverage)) {
                if (diffAverage <= -1)
                    row[4].setCssClass("perfcharts-unit-ms2s perfcharts-better");
                else if (diffAverage >= 1)
                    row[4].setCssClass("perfcharts-unit-ms2s perfcharts-worse");
                else {
                    row[4].setCssClass("perfcharts-unit-ms2s");
                }
            }
            double diffAveragePercentage = diffAverage * 100.0 / dAverage;
            row[5] = new TableCell(diffAveragePercentage);
            if (Double.isFinite(diffAveragePercentage)) {
                if (diffAveragePercentage <= -0.001)
                    row[5].setCssClass("perfcharts-better");
                else if (diffAveragePercentage >= 0.001)
                    row[5].setCssClass("perfcharts-worse");
            }
            /*if (Double.isInfinite(diffAveragePercentage)|| diffAveragePercentage >= 50.0){
                row[5].setCssClass("perfcharts_warning");
				//row[0].setCssClass("perfcharts_warning");
			} else if (diffAveragePercentage <= -50.0){
                row[5].setCssClass("perfcharts_fine");
            }*/
//
//            double s90Line = (double) s90LineField.select(dataRow);
//            double d90Line = (double) d90LineField.select(dataRow);
//            row[6] = new TableCell(s90Line);
//            row[7] = new TableCell(s90Line - d90Line);
//            row[8] = new TableCell((s90Line - d90Line) * 100.0 / d90Line);
            //double sThroughput = (double) sThroughputField.select(dataRow);
            //double dThroughput = (double) dThroughputField.select(dataRow);
//            row[9] = new TableCell(sThroughput);
//            row[10] = new TableCell(sThroughput - dThroughput);
//            row[11] = new TableCell((sThroughput - dThroughput) * 100.0
//                    / dThroughput);
            double sError = (double) sErrorField.select(dataRow);
            double dError = (double) dErrorField.select(dataRow);
            row[6] = new TableCell(sError);
            if (Double.isInfinite(sError) || Double.isNaN(sError)
                    || sError > 0.0) {
                row[6].setCssClass("perfcharts-warning");
                row[0].setCssClass("perfcharts-warning");
            }
            double diffError = sError - dError;
            row[7] = new TableCell(diffError);
            if (Double.isFinite(diffError)) {
                if (diffError <= -0.001)
                    row[7].setCssClass("perfcharts-better");
                else if (diffError >= 0.001)
                    row[7].setCssClass("perfcharts-worse");
            }

        }
        List<TableCell[]> rows = new ArrayList<TableCell[]>(tx2RowMap.size());
        for (Map.Entry<String, TableCell[]> entry : tx2RowMap.entrySet())
            rows.add(entry.getValue());
        // rows.add(totalRow);
        table.setRows(rows);
        if (totalRow != null) {
            List<TableCell[]> bottomRows = new ArrayList<TableCell[]>(1);
            bottomRows.add(totalRow);
            table.setBottomRows(bottomRows);
        }
        return table;
    }

    public PerformanceSimpleComparisonTableConfig getConfig() {
        return config;
    }

    public void setConfig(PerformanceSimpleComparisonTableConfig config) {
        this.config = config;
    }

}
