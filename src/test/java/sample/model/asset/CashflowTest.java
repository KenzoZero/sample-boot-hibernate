package sample.model.asset;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.*;

import sample.*;

//low: 簡易な正常系検証が中心。依存するCashBalanceの単体検証パスを前提。
public class CashflowTest extends EntityTestSupport {

	@Override
	protected void setupPreset() {
		targetEntities(Cashflow.class, CashBalance.class);
	}
	
	@Test
	public void register() {
		String baseDay = businessDay.day();
		String baseMinus1Day = businessDay.day(-1);
		String basePlus1Day = businessDay.day(1);
		tx(() -> {
			// 過去日付の受渡でキャッシュフロー発生 [例外]
			try {
				Cashflow.register(rep, fixtures.cfReg("test1", "1000", baseMinus1Day));
				fail();
			} catch (ValidationException e) {
				assertThat(e.getMessage(), is("error.Cashflow.beforeEqualsDay"));
			}
			// 翌日受渡でキャッシュフロー発生
			assertThat(Cashflow.register(rep, fixtures.cfReg("test1", "1000", basePlus1Day)),
				allOf(
					hasProperty("amount", is(new BigDecimal("1000"))),
					hasProperty("statusType", is(ActionStatusType.UNPROCESSED)),
					hasProperty("eventDay", is(baseDay)),
					hasProperty("valueDay", is(basePlus1Day))));
		});
	}

	@Test
	public void realize() {
		String baseDay = businessDay.day();
		String baseMinus1Day = businessDay.day(-1);
		String baseMinus2Day = businessDay.day(-2);
		String basePlus1Day = businessDay.day(1);
		tx(() -> {
			CashBalance.getOrNew(rep, "test1", "JPY");
			
			// 未到来の受渡日 [例外]
			Cashflow cfFuture = fixtures.cf("test1", "1000", baseDay, basePlus1Day).save(rep);
			try {
				cfFuture.realize(rep);
				fail();
			} catch (ValidationException e) {
				assertThat(e.getMessage(), is("error.Cashflow.realizeDay"));
			}
			
			// キャッシュフローの残高反映検証。  0 + 1000 = 1000
			Cashflow cfNormal = fixtures.cf("test1", "1000", baseMinus1Day, baseDay).save(rep);
			assertThat(cfNormal.realize(rep), hasProperty("statusType", is(ActionStatusType.PROCESSED)));
			assertThat(CashBalance.getOrNew(rep, "test1", "JPY"),
				hasProperty("amount", is(new BigDecimal("1000"))));
			
			// 処理済キャッシュフローの再実現 [例外]
			try {
				cfNormal.realize(rep);
				fail();
			} catch (ValidationException e) {
				assertThat(e.getMessage(), is("error.ActionStatusType.unprocessing"));
			}
			
			// 過日キャッシュフローの残高反映検証。 1000 + 2000 = 3000
			Cashflow cfPast = fixtures.cf("test1", "2000", baseMinus2Day, baseMinus1Day).save(rep);
			assertThat(cfPast.realize(rep), hasProperty("statusType", is(ActionStatusType.PROCESSED)));
			assertThat(CashBalance.getOrNew(rep, "test1", "JPY"),
				hasProperty("amount", is(new BigDecimal("3000"))));
		});
	}
	
	@Test
	public void registerWithRealize() {
		String baseDay = businessDay.day();
		tx(() -> {
			CashBalance.getOrNew(rep, "test1", "JPY");
			// 発生即実現
			Cashflow.register(rep, fixtures.cfReg("test1", "1000", baseDay));
			assertThat(CashBalance.getOrNew(rep, "test1", "JPY"),
				hasProperty("amount", is(new BigDecimal("1000"))));
		});
	}

}
