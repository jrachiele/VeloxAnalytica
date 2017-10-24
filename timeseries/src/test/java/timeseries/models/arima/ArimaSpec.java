/*
 * Copyright (c) 2017 Jacob Rachiele
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to
 * do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *
 * Jacob Rachiele
 */

package timeseries.models.arima;

import data.DoubleFunctions;
import timeseries.TestData;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import timeseries.TimePeriod;
import timeseries.TimeSeries;
import timeseries.models.Forecast;
import timeseries.models.arima.Arima.Constant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertArrayEquals;

public class ArimaSpec {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void whenZerosThenFittedAreZero() throws InterruptedException {
        TimeSeries timeSeries = new TimeSeries(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        double[] expected = new double[timeSeries.size()];
        ArimaOrder order = ArimaOrder.order(1, 0, 1, 0, 0, 0, Constant.INCLUDE);
        Arima arimaModel = Arima.model(timeSeries, order, ArimaModel.FittingStrategy.CSS);
        assertThat(arimaModel.fittedSeries().asArray(), is(expected));
    }

    @Test
    public void whenArimaModelFitThenParametersSimilarToROutput() throws Exception {
        TimeSeries series = TestData.livestock;
        ArimaOrder order = ArimaOrder.order(1, 1, 1);
        Arima model = Arima.model(series, order, TimePeriod.oneYear(), Arima.FittingStrategy.CSSML);
        assertThat(model.coefficients().arCoeffs()[0], is(closeTo(0.64, 0.02)));
        assertThat(model.coefficients().maCoeffs()[0], is(closeTo(-0.50, 0.02)));
    }

    @Test
    public void whenArimaModelFitDebitcardsThenParametersSimilarToROutput() throws Exception {
        TimeSeries series = TestData.debitcards;
        ArimaOrder order = ArimaOrder.order(1, 1, 1, 1, 1, 1);
        Arima model = Arima.model(series, order, TimePeriod.oneYear(), Arima.FittingStrategy.CSSML);

        ArimaCoefficients expected = ArimaCoefficients.newBuilder()
                                                      .setARCoeffs(-0.1040)
                                                      .setMACoeffs(-0.6214)
                                                      .setSeasonalARCoeffs(0.0051)
                                                      .setSeasonalMACoeffs(-0.5713)
                                                      .setDifferences(1)
                                                      .setSeasonalDifferences(1)
                                                      .setSeasonalFrequency(12)
                                                      .build();
        assertArrayEquals(expected.getAllCoeffs(), model.coefficients().getAllCoeffs(), 1E-2);
    }

    @Test
    public void whenArimaModelForecastThenForecastValuesCorrect() throws Exception {
        TimeSeries series = TestData.livestock;
        ArimaCoefficients coeffs = ArimaCoefficients.newBuilder().setARCoeffs(0.6480679).setMACoeffs(-0.5035514).
                setDifferences(1).build();
        Arima model = Arima.model(series, coeffs, TimePeriod.oneYear());
        double[] expected = {457.660172, 458.904464, 459.71085, 460.233443, 460.572118, 460.791603, 460.933844,
                461.026026, 461.085766, 461.124482};
        assertArrayEquals(expected, model.fcst(10), 1E-4);
    }

    @Test
    public void whenArimaModelForecastThenPredictionLevelsAccurate() throws Exception {
        TimeSeries series = TestData.livestock;
        ArimaCoefficients coeffs = ArimaCoefficients.newBuilder()
                                                    .setARCoeffs(0.6480679 )
                                                    .setMACoeffs(-0.5035514)
                                                    .setDifferences(1)
                                                    .build();
        Arima model = Arima.model(series, coeffs, TimePeriod.oneYear(), ArimaModel.FittingStrategy.CSSML);
        Forecast fcst = model.forecast(10);
        double[] expectedLower = {432.515957, 420.689242, 410.419267, 401.104152, 392.539282, 384.606261, 377.216432,
                370.29697, 363.786478, 357.632926
        };
        double[] expectedUpper = {482.804388, 497.119686, 509.002433, 519.362733, 528.604955, 536.976945, 544.651257,
                551.755082, 558.385054, 564.616037
        };
        double[] actualLower = fcst.computeLowerPredictionBounds(10, 0.05).asArray();
        double[] actualUpper = fcst.computeUpperPredictionBounds(10, 0.05).asArray();
        assertArrayEquals(expectedLower, actualLower, 1E-4);
        assertArrayEquals(expectedUpper, actualUpper, 1E-4);
    }

    @Test
    public void whenModelFitThenModelInformationCorrect() {
        ArimaCoefficients coefficients = ArimaCoefficients.newBuilder()
                                                          .setARCoeffs(-0.5)
                                                          .setMACoeffs(-0.5)
                                                          .setDifferences(1)
                                                          .build();
        Arima model = Arima.model(TestData.livestock, coefficients, ArimaModel.FittingStrategy.ML);
        assertThat(model.sigma2(), is(closeTo(531.8796, 1E-4)));
        assertThat(model.logLikelihood(), is(closeTo(-210.1396, 1E-4)));
        assertThat(model.aic(), is(closeTo(426.2792, 1E-4)));
    }

    @Test
    public void whenModelFitThenCorrectDrift() {
        ArimaCoefficients coefficients = ArimaCoefficients.newBuilder()
                                                          .setDrift(4.85376578)
                                                          .setARCoeffs(0.01803952)
                                                          .setDifferences(1)
                                                          .build();
        Arima model = Arima.model(TestData.livestock, coefficients, ArimaModel.FittingStrategy.ML);
        assertThat(model.coefficients().drift(), is(closeTo(4.85376578, 1E-4)));
    }

    @Test
    public void testEqualsAndHashCode() throws InterruptedException {
        TimeSeries series = TestData.livestock;
        ArimaOrder order = ArimaOrder.order(1, 1, 1);
        ArimaOrder order2 = ArimaOrder.order(1, 0, 1, Constant.INCLUDE);
        Arima model1 = Arima.model(series, order);
        Arima model2 = Arima.model(series, order2, ArimaModel.FittingStrategy.CSS);
        assertThat(model1, is(model1));
        assertThat(model1, is(not(new Object())));
        assertThat(model1.equals(null), is(false));
        assertThat(model1, is(not(model2)));

        Arima model3 = Arima.model(series, order);
        assertThat(model1, is(model3));
        assertThat(model1.hashCode(), is(model3.hashCode()));
    }

    @Test
    public void testModelInfoEqualsAndHashCode() {
        ArimaModel.ModelInformation info1 = new ArimaModel.ModelInformation(2, 50.0, -100.0,
                                                                            DoubleFunctions.arrayFrom(),
                                                                            DoubleFunctions.arrayFrom());
        ArimaModel.ModelInformation info2 = new ArimaModel.ModelInformation(2, 45.0, -90.0,
                                                                            DoubleFunctions.arrayFrom(),
                                                                            DoubleFunctions.arrayFrom());
        ArimaModel.ModelInformation info3 = new ArimaModel.ModelInformation(2, 50.0, -100.0,
                                                                            DoubleFunctions.arrayFrom(),
                                                                            DoubleFunctions.arrayFrom());
        assertThat(info1, is(info1));
        assertThat(info1.hashCode(), is(info3.hashCode()));
        assertThat(info1, is(info3));
        assertThat(info1, is(not(info2)));
        assertThat(info1, is(not(new Object())));
        assertThat(info1.equals(null), is(false));
    }

}
