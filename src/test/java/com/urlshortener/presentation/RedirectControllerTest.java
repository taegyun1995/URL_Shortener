package com.urlshortener.presentation;

import com.urlshortener.application.ShortKeyNotFoundException;
import com.urlshortener.application.UrlService;
import com.urlshortener.domain.ShortKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RedirectController.class)
@ActiveProfiles("test")
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Test
    void 등록된_shortKey_조회시_302_리다이렉트한다() throws Exception {
        given(urlService.resolve(ShortKey.of("abc1234")))
                .willReturn("https://example.com/long");

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/long"));
    }

    @Test
    void 없는_shortKey_조회시_404를_반환한다() throws Exception {
        ShortKey unknown = ShortKey.of("zzzzzzz");
        given(urlService.resolve(unknown))
                .willThrow(new ShortKeyNotFoundException(unknown));

        mockMvc.perform(get("/zzzzzzz"))
                .andExpect(status().isNotFound());
    }

    @Test
    void 잘못된_형식의_shortKey_조회시_400을_반환한다() throws Exception {
        // 'abcdef_'는 7자리지만 '_'는 Base62 외 문자 → ShortKey.of()에서 IAE
        mockMvc.perform(get("/abcdef_"))
                .andExpect(status().isBadRequest());
    }
}
