package sample.webflux;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.dictionary.CustomDictionary;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class HanlpController {
	@GetMapping(path = "/hanlp",  produces = MediaType.APPLICATION_JSON_VALUE)

	@ResponseBody
	public Object[] segment(@RequestParam String sentence) {
		 return HanLP.segment(sentence).stream().map(term->term.word).toArray();
	}
}
