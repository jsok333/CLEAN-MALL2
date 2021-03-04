package cleanassign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import cleanassign.config.kafka.KafkaProcessor;

@Service
public class CleanassignPolicyHandler {
	@Autowired
	CleanassignRepository cleanassignRepository;

	@StreamListener(KafkaProcessor.INPUT)
	public void onStringEventListener(@Payload String eventString) {

	}

	private static String[][] driverBank = { { "백영곤", "010-2345-6789", "34가4567" },
			{ "안채우", "010-3345-7789", "44나4567" }, { "임광현", "010-4345-8789", "54다4567" },
			{ "장윤정", "010-5345-9789", "64라4567" }, { "옥준삼", "010-6345-0789", "74마4567" },
			{ "유승오", "010-7345-1789", "84사4567" } };

	public static CleanassignCompleted getCleanassign() {
		CleanassignCompleted cleanassignCompleted = new CleanassignCompleted();

		int randDriver = (int) (Math.random() * 6);
		cleanassignCompleted.setCleanman(driverBank[randDriver][0]);
		cleanassignCompleted.setCleanmantel(driverBank[randDriver][1]);
		cleanassignCompleted.setCleanmanid(driverBank[randDriver][2]);
		return cleanassignCompleted;
	}

	// private String 호출상태; //호출,호출중,호출확정,호출취소
	@StreamListener(KafkaProcessor.INPUT)
	public void wheneverCleanmanageAssigned_(@Payload CleanmanageAssigned cleanmanageAssigned) {
		System.out.println("##### EVT TYPE[CleanmanageAssigned]  : " + cleanmanageAssigned.getEventType());
		if (cleanmanageAssigned.isMe()) {
			System.out.println("##### listener  : " + cleanmanageAssigned.toJson());

			if (cleanmanageAssigned.getStatus() != null && cleanmanageAssigned.getStatus().equals("호출중")) {

				cleanmanageAssigned.setStatus("호출확정");
				// CleanassignCompleted CleanassignCompleted = Assigner.get택시할당됨();
				// BeanUtils.copyProperties(CleanmanageAssigned, CleanassignCompleted);
				// CleanassignCompleted.setEventType("CleanassignCompleted");
				cleanmanageAssigned.publish();

				CleanassignCompleted cleanassignCompleted = getCleanassign();
				cleanassignCompleted.setId(cleanmanageAssigned.getId());
				cleanassignCompleted.setAssignstatus("할당확정");
				cleanassignCompleted.setTel(cleanmanageAssigned.getTel());
				cleanassignCompleted.setLocation(cleanmanageAssigned.getLocation());
				cleanassignCompleted.setEventType("CleanassignCompleted");
				// CleanmanageAssigned.publishAfterCommit();
				cleanassignCompleted.publish();
			}
		}
	}

	@StreamListener(KafkaProcessor.INPUT)
	public void wheneverCleanassignCompleted_(@Payload CleanassignCompleted cleanassignCompleted) {
		System.out.println("##### EVT TYPE[CleanassignCompleted]  : " + cleanassignCompleted.getEventType());
		if (cleanassignCompleted.isMe()) {
			System.out.println("##### listener  : " + cleanassignCompleted.toJson());

			if (cleanassignCompleted.getAssignstatus() != null && cleanassignCompleted.getAssignstatus().equals("할당확정")) {

//            	CleanassignCompleted CleanassignCompleted = Assigner.get택시할당됨();
//            	BeanUtils.copyProperties(CleanmanageAssigned, CleanassignCompleted);
//            	
//                //CleanassignCompleted.setEventType("CleanassignCompleted");
//            	CleanassignCompleted.setEventType("CleanassignCompleted");
//            	//CleanmanageAssigned.publishAfterCommit();
//            	CleanassignCompleted.publish();
			}
		}
	}

	@StreamListener(KafkaProcessor.INPUT)
	public void wheneverCleanmanageCancelled_(@Payload CleanmanageCancelled cleanmanageCancelled) {

		if (cleanmanageCancelled.isMe()) {
			System.out.println("##### listener  : " + cleanmanageCancelled.toJson());

			cleanassignRepository.findById(Long.valueOf(cleanmanageCancelled.getId())).ifPresent((택시호출) -> {
				택시호출.setStatus("할당취소");
				cleanassignRepository.save(택시호출);
			});

			cleanmanageCancelled.publish();
		}
	}

}
