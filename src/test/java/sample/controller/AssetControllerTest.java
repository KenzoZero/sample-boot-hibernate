package sample.controller;

import static org.mockito.BDDMockito.*;

import java.util.*;

import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import sample.WebTestSupport;
import sample.model.asset.CashInOut;
import sample.model.asset.CashInOut.RegCashOut;
import sample.usecase.AssetService;

@WebMvcTest(AssetController.class)
public class AssetControllerTest extends WebTestSupport {

    @MockBean
    private AssetService service;
    
    @Override
    protected String prefix() {
        return "/api/asset";
    }

    @Test
    public void findUnprocessedCashOut() {
        given(service.findUnprocessedCashOut()).willReturn(resultCashOuts());
        performGet("/cio/unprocessedOut/",
            JsonExpects.success()
                .match("$[0].currency", "JPY")
                .match("$[0].absAmount", 3000)
                .match("$[1].absAmount", 4000));
    }

    private List<CashInOut> resultCashOuts() {
        return Arrays.asList(
                fixtures.cio("sample", "3000", true),
                fixtures.cio("sample", "4000", true));
    }

    @Test
    public void withdraw() {
        given(service.withdraw(any(RegCashOut.class))).willReturn(1L);
        performPost(
          uriBuilder("/cio/withdraw")
            .queryParam("accountId", "sample")
            .queryParam("currency", "JPY")
            .queryParam("absAmount", "1000")
            .build(),
          JsonExpects.success()
        );
    }

}
