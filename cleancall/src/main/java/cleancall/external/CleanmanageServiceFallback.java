package cleancall.external;

import org.springframework.stereotype.Component;

@Component
public class CleanmanageServiceFallback implements CleanmanageService {
	 
	@Override
	public void cleanManageCall( Cleanmanage cleanmanage) {
		System.out.println("Circuit breaker has been opened. Fallback returned instead. " + cleanmanage.getId());
	}

}
