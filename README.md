![image](https://user-images.githubusercontent.com/78134087/109758190-882e2f00-7c2e-11eb-805a-2ad865b3a75f.png)


# 서비스 시나리오

## 기능적 요구사항
1. 고객이 청소종류와 청소 희망일 선택하여 청소서비스 신청을 요청한다.
3. 가용한 청소담당자를 확인하여 배정을 요청한다
4. 대상자 중에서 배정한다.
5. 배정되면 고객에게 신청완료 정보를 전달한다.
6. 고객은 신청된 청소를 취소한다.
8. 신청이 취소되면 배정도 취소된다
9. 고객이 신청내역을 확인할 수 있다


## 비기능적 요구사항
1. 트랜잭션
    1. 담당자배정 요청이 되지 않으면 신청을 할 수 없다. → Sync 호출
1. 장애격리
    1. 신청은 담당자 배정 기능이 동작하지 않더라고 호출이 가능해야 한다 → Async (event-driven), Eventual Consistency
    1. 배정요청이 과중되면 신청요청을 잠시동안 받지 않고 결제를 잠시후에 하도록 유도한다 → Circuit breaker, fallback
1. 성능
    1. 고객은  청소서비스 신청상태를 확인할 수 있어야 한다 → CQRS, Event driven 

# 체크포인트

1. Saga
1. CQRS
1. Correlation
1. Req/Resp
1. Gateway
1. Deploy/ Pipeline
1. Circuit Breaker
1. Autoscale (HPA)
1. Zero-downtime deploy (Readiness Probe)
1. Config Map/ Persistence Volume
1. Polyglot
1. Self-healing (Liveness Probe)


# 분석/설계


## AS-IS 조직 (Horizontally-Aligned)
  ![image](https://user-images.githubusercontent.com/487999/79684144-2a893200-826a-11ea-9a01-79927d3a0107.png)

## TO-BE 조직 (Vertically-Aligned)
  ![조직구조](https://user-images.githubusercontent.com/78134019/109453964-977a7480-7a96-11eb-83cb-5445c363a9e8.jpg)


## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/xLFKypBQXvWvzYlK2R6oICI6wxX2/mine/4e94bb2943d70a4d8ccf4767432d069d


### 이벤트 도출
![image](https://user-images.githubusercontent.com/78134087/109794393-29ca7600-7c59-11eb-9577-98fa2c3c3231.png)


### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/78134087/109794485-3f3fa000-7c59-11eb-9d1e-ae84074db59f.png)


과정 중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함

- 청소종류 선택됨, 청소희망일 선택됨 : UI 의 이벤트이지, 업무적인 의미의 이벤트가 아니라서 제외

- 청소가능 담당자 확인됨 :  계획된 사업 범위 및 프로젝트에서 벗어서난다고 판단하여 제외

	

### 액터, 커맨드 부착하여 읽기 좋게
![4](https://user-images.githubusercontent.com/78134087/109797117-5d5acf80-7c5c-11eb-9434-af50d409764f.JPG)


### 어그리게잇으로 묶기
![5어그릿](https://user-images.githubusercontent.com/78134087/109798327-f5a58400-7c5d-11eb-8fd1-d3a37d4ebcb8.JPG)

 - 청소신청, 청소관리, 청소배정 어그리게잇을 생성하고 그와 연결된 command 와 event 들에 의하여 트랜잭션이 유지되어야 하는 단위로 그들 끼리 묶어줌 


### 바운디드 컨텍스트로 묶기
![6바운디드](https://user-images.githubusercontent.com/78134087/109798348-fb02ce80-7c5d-11eb-9123-edc8fb12777b.JPG)


### 폴리시 부착 (괄호는 수행주체, 폴리시 부착을 둘째단계에서 해놔도 상관 없음. 전체 연계가 초기에 드러남)
![7폴리시](https://user-images.githubusercontent.com/78134087/109798382-05bd6380-7c5e-11eb-90a1-a2c5bf382415.JPG)


### 폴리시의 이동
![8이동](https://user-images.githubusercontent.com/78134087/109798397-0a821780-7c5e-11eb-87d7-c40c263b3a9c.JPG)


### 컨텍스트 매핑 (점선은 Pub/Sub, 실선은 Req/Resp)
![9매핑](https://user-images.githubusercontent.com/78134087/109798415-0fdf6200-7c5e-11eb-85bc-191f904c341d.JPG)



### 완성된 모형

![10완성](https://user-images.githubusercontent.com/30484527/110022969-eadb1400-7d6f-11eb-8117-c6bda37d3fe1.JPG)


### 기능적 요구사항 검증

![11기능](https://user-images.githubusercontent.com/30484527/110023086-152cd180-7d70-11eb-8e9b-ab2fcb79c1d8.JPG)



신청
- 고객이 청소를 신청요청한다.(ok)
- 청소관리 시스템이 청소 배정을 요청한다.(ok)
- 청소담당자 배정이 완료된다.(ok)
- 신청상태를 갱신한다.(ok)
- 고객이 변경 내역을 조회한다.(ok)


취소
- 고객이 청소신청 취소요청한다.(ok)
- 청소관리 시스템이 배정 취소를 요청한다.(ok)
- 청소담당자 배정이 취소된다.(ok)
- 취소상태로 갱신한다.(ok)
- 고객이 취소 내역을 조회한다.(ok)


- 고객이 진행내역을 볼 수 있어야 한다. (ok)


### 비기능 요구사항 검증

![12비기능 (1)](https://user-images.githubusercontent.com/30484527/110024793-e1eb4200-7d71-11eb-8e29-9c0b61e23802.JPG)


마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리

 
1) 청소 배정요청이 완료되지 않은 신청요청 완료처리는 최종 배정이 되지 않는 경우 
   무한정 대기 등 대고객 서비스 및 신뢰도에 치명적 문제점이 있어 ACID 트랜잭션 적용. 
    신청요청 시 청소 배정요청에 대해서는 Request-Response 방식 처리

2) 신청요청 완료시 배정확인 및 결과 전송: cleanmanage 서비스에서
   cleanassign 마이크로서비스로 청소배정 요청이 전달되는 과정에 있어서 
   cleanassign 마이크로서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
 
3) 나머지 모든 inter-microservice 트랜잭션: 신청상태, 배정/배정취소 등 이벤트에 대한 상태변경을 처리하여 
   데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency를 기본으로 채택함. 

## 헥사고날 아키텍처 다이어그램 도출 (Polyglot)
![11헥사](https://user-images.githubusercontent.com/78134087/109798519-31d8e480-7c5e-11eb-826e-9cd8160d0fa9.JPG)


# 구현:

서비스를 로컬에서 실행하는 방법은 아래와 같으며, 실행의 편의성을 위해서
각 서비스별로 bat 파일로 묶어서 실행 합니다.

```
- run_cleancall.bat
call setenv.bat
cd ..\cleanmall\cleancall
mvn clean spring-boot:run
pause ..

- run_cleanmanage.bat
call setenv.bat
cd ..\cleanmall\cleanmanage
mvn clean spring-boot:run
pause ..

- run_cleanassign.bat
call setenv.bat
cd ..\cleanmall\cleanassign
mvn clean spring-boot:run
pause ..

- run_customer.bat
call setenv.bat
SET CONDA_PATH=%ANACONDA_HOME%;%ANACONDA_HOME%\BIN;%ANACONDA_HOME%\condabin;%ANACONDA_HOME%\Library\bin;%ANACONDA_HOME%\Scripts;
SET PATH=%CONDA_PATH%;%PATH%;
cd ..\cleanmall\customer\
python policy-handler_local.py 
pause ..

```

## DDD 의 적용
총 3개의 Domain 으로 관리되고 있으며, 청소서비스신청(CleanCall) , 청소시스템 관리(CleanManage), 청소담당자배정(CleanAssign) 으로 구성하였습니다.


![1  ddd 화면 캡처 2021-03-04 235102](https://user-images.githubusercontent.com/61448505/109982633-6d9aa980-7d45-11eb-9a06-79b1cb861da6.jpg)

![1 ddd 2화면 캡처 2021-03-04 235257](https://user-images.githubusercontent.com/61448505/109982778-915def80-7d45-11eb-8fbc-cfc46c1d330a.jpg)



## 폴리글랏 퍼시스턴스

```
위치 : /cleanmall>cleancall>pom.xml
```
![2 poly 화면 캡처 2021-03-04 235543](https://user-images.githubusercontent.com/61448505/109983126-e1d54d00-7d45-11eb-8b3c-b673b72161d9.jpg)

## 폴리글랏 프로그래밍 - 파이썬

- 로컬 용 소스
```
위치 : /cutomer>policy-handler_local.py
```
![3 local_py 화면 캡처 2021-03-05 000324](https://user-images.githubusercontent.com/61448505/109983569-498b9800-7d46-11eb-962b-6d0bf0e91632.jpg)

- 클라우드 용 소스

![3 local_py 화면 캡처 2021-03-05 000324](https://user-images.githubusercontent.com/61448505/109983846-91aaba80-7d46-11eb-952d-2c811ecce64b.jpg)

## 마이크로 서비스 호출 흐름

- cleancall 서비스 호출처리

청소서비스 요청(cleancall)->청소시스템 관리(CleanManage) 우선 호출처리(req/res) 되며,
청소담당자 할당(cleanassign)에서 청소담당자를 할당하게 되면 호출 상태가 호출에서 호출확정 상태가 됩니다

우선, 로컬에서는 다음과 같이 두 개의 호출 상태를 만듭니다.
```
http localhost:8081/cleancalls tel="01023221353" location="seoul" cost=200000
http localhost:8081/cleancalls tel="0102545145" location="seoul" cost=400000

```
![3  청소요청1 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/61448505/109984974-a176ce80-7d47-11eb-82b5-16ebe0ae5eaa.jpg)

![3  청소요청1 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/61448505/109985170-cff4a980-7d47-11eb-8b81-6d3cfed26a5a.jpg)


우선, 클라우드 상에서 호출은 다음과 같이 합니다. External-IP는 52.231.8.44 입니다.
```
http 52.231.8.44:8080/cleancalls tel="0102545145" location="seoul" cost=600000
http 52.231.8.44:8080/cleancalls tel="0102545145" location="seoul" cost=900000000
```

![3  청소요청 3 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/61448505/109991473-b2c2d980-7d4d-11eb-96e4-bef782653055.jpg)

![3  청소요청 4 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/61448505/109991977-282eaa00-7d4e-11eb-8527-0d224413d1ba.jpg)



아래 호출 결과는 모두 청소담당자 할당(cleanassign)에서 청소담당자가 할당되어 호출 서비스를 확인하연 호출 상태는 호출 확정가 되어 있습니다.

![3  청소요청 33 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/30484527/110019453-d39a2780-7d6b-11eb-8270-11f07b3f7517.jpg)

클라우드 상에서도 마찬가지 입니다.

![3  청소요청 5 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/61448505/109992627-c9b5fb80-7d4e-11eb-989f-c35b127d3920.jpg)

- cleancall 서비스 호출 취소 처리

호출 취소는 청소서비스 호출에서 다음과 같이 호출 하나를 취소 함으로써 진행 합니다.

```
http delete http://localhost:8081/cleancalls/2
HTTP/1.1 204
Date: Thu, 04 Mar 2021 15:21:28 GMT
```

클라우드 상에서 호출 취소는 다음과 같습니다.
```
http delete http://20.194.36.201:8080/cleancalls/4
HTTP/1.1 204
Date: Thu, 04 Mar 2021 16:07:12 GMT
```


호출이 취소 되면 아래와 같이 청소서비스 호출이 하나가 삭제 되었고,

```
http localhost:8081/cleancalls
http 52.231.8.44:8080/cleancalls
```

![4  삭제 화면 캡처 2021-03-05 002356](https://user-images.githubusercontent.com/61448505/109986485-231b2c00-7d49-11eb-9dbe-e07c96197e07.jpg)

![3  청소요청 7 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/61448505/109994190-67f69100-7d50-11eb-8b09-a0dccdf8555f.jpg)

청소시스템 관리에서는 해당 호출의 호출 상태가 호출취소로 상태가 변경 됩니다.

```
http localhost:8082/cleanmanages
http 52.231.8.44:8080/cleancalls
```

![4  삭제 화면 캡처 2021-03-05 002356](https://user-images.githubusercontent.com/61448505/109987221-c704d780-7d49-11eb-913f-9fb2cabd1731.jpg)

![3  청소요청 6 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/61448505/109993444-a5a6ea00-7d4f-11eb-80eb-13dfab35cb76.jpg)


- 고객 메시지 서비스 처리

고객(customer)는 호출 확정과 할당 확정에 대한 메시지를 다음과 같이 받을 수 있으며,
할당 된 택시기사의 정보를 또한 확인 할 수 있습니다.
파이썬으로 구현 하였습니다.

![3  청소요청 9 화면 캡처 2021-03-05 001251](https://user-images.githubusercontent.com/30484527/110021379-fd544e00-7d6d-11eb-9eaf-9848386adee0.jpg)

## Gateway 적용

서비스에 대한 하나의 접점을 만들기 위한 게이트웨이의 설정은 8080이며,
청소서비스신청(CleanCall) , 청소시스템 관리(CleanManage), 청소담당자배정(CleanAssign) 마이크로서비스에 대한 일원화 된 접점을 제공하기 위한 설정 입니다.
```
청소서비스신청 서비스 : 8081
청소시스템 관리 서비스 : 8082
청소담당자배정 서비스 : 8083
```

gateway > applitcation.yml 설정

![5  gateway 1 화면 캡처 2021-03-05 003252](https://user-images.githubusercontent.com/61448505/109987908-5f02c100-7d4a-11eb-81c5-3eb7a64414ad.jpg)

![5  gateway 2 화면 캡처 2021-03-05 003252](https://user-images.githubusercontent.com/61448505/109988271-b86af000-7d4a-11eb-80d1-08ecac5811ee.jpg)



- gateway 로컬 테스트

로컬 테스트는 다음과 같이 한글 서비스 호출로 테스트 되었습니다.

```
http localhost:8080/cleancalls
-> gateway 를 호출하나 8081 로 호출됨

```
![gateway_3](https://user-images.githubusercontent.com/78134019/109480424-da504280-7abe-11eb-988e-2a6d7a1f7cea.png)



## 동기식 호출 과 Fallback 처리

청소서비스신청(CleanCall)과 청소시스템 관리(CleanManage)간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하였습니다.
호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

로컬 테스트를 위한 파일은 다음과 같이 구현 하였으며,
```
# external > CleanmanageService.java


package cleancall.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

//@FeignClient(name="taximanage", url="http://localhost:8082")
@FeignClient(name="cleanmanage", url="http://localhost:8082", fallback = CleanmanageServiceFallback.class)
public interface CleanmanageService {

    @RequestMapping(method= RequestMethod.POST, path="/cleanmanages")
    public void cleanManageCall(@RequestBody Cleanmanage cleanmanage);

}

```


다음은 CleanmanageService 인터페이스를 구현한 CleanmanageServiceFallback 클래스입니다.

```
package cleancall.external;

import org.springframework.stereotype.Component;

@Component
public class CleanmanageServiceFallback implements CleanmanageService {
	 
	@Override
	public void cleanManageCall( Cleanmanage cleanmanage) {
		System.out.println("Circuit breaker has been opened. Fallback returned instead. " + cleanmanage.getId());
	}

}

```

![7 동기처리 오류 화면 캡처 2021-03-05 024343](https://user-images.githubusercontent.com/30484527/110006288-cd507f00-7d5c-11eb-8ba6-16b532e159b9.jpg)

- 로컬 청소서비스 신청 

청소서비스 신청을 하면 청소서비스 관리에 해당 요청에 대해 다음과 같이 동기적으로 진행 합니다.
```
# Cleancall.java

@PostPersist
    public void onPostPersist(){
        Cleancalled Cleancalled = new Cleancalled();
        BeanUtils.copyProperties(this, Cleancalled);
        Cleancalled.publishAfterCommit();
    	
    	System.out.println("고객 휴대폰번호 " + getTel());
        System.out.println("location " + getLocation());
        System.out.println("호출상태 " + getStatus());
        System.out.println("Cost " + getCost());
        //Following code causes dependency to external APIs
        // it is NOT A GOOD PRACTICE. instead, Event-Policy mapping is recommended.   	
    	if(getTel() != null)
		{
    		System.out.println("SEND###############################" + getId());
			Cleanmanage cleanmanage = new Cleanmanage();
			cleanmanage.setId(getId());
			cleanmanage.setOrderId(String.valueOf(getId()));
			cleanmanage.setTel(getTel());
	        if(getLocation()!=null)
				cleanmanage.setLocation(getLocation());
	        if(getStatus()!=null)
				cleanmanage.setStatus(getStatus());
	        if(getCost()!=null)
				cleanmanage.setCost(getCost());
	        
	        // mappings goes here
	        CleancalllApplication.applicationContext.getBean(CleanmanageService.class).cleanManageCall(cleanmanage);
		}
		
```
![7 동기처리 오류 2 화면 캡처 2021-03-05 024343](https://user-images.githubusercontent.com/30484527/110009515-7482e580-7d60-11eb-989f-c4d695bd7e9b.jpg)

```

```
- 동기식 호출 적용으로 청소서비스 관리시스템이 정상적이지 않으면 , 청소서비스 신청도 접수될 수 없음을 다음과 같이 확인 할 수 있습니다.

```
- 청소서비스 관리시스템 down 후 Cleancall 신청 
#Cleancall

```
![7 동기처리 오류 3 화면 캡처 2021-03-05 024343](https://user-images.githubusercontent.com/30484527/110009227-135b1200-7d60-11eb-97da-0df04aacc905.jpg)

```
# 청소서비스 관리시스템 (cleanmanage) 재기동 후 호출

```
![7 동기처리 오류 4 화면 캡처 2021-03-05 024343](https://user-images.githubusercontent.com/30484527/110009914-038ffd80-7d61-11eb-87da-e733dd9da9b6.jpg)

![7 동기처리 오류 5 화면 캡처 2021-03-05 024343](https://user-images.githubusercontent.com/30484527/110010544-c1b38700-7d61-11eb-8afb-043cbc4bd9d4.jpg)

-fallback

![7 동기처리 오류 6 화면 캡처 2021-03-05 024343](https://user-images.githubusercontent.com/30484527/110011203-836a9780-7d62-11eb-8f93-c8804ecb9f7f.jpg)

## 비동기식 호출 / 장애격리  / 성능

청소시스템 관리(CleanManage)와 청소담당자 배정(CleanAssign)은 비동기식 처리이므로 ,
청소서비스신청(CleanCall) 서비스 호출에는 영향이 없도록 구성 합니다.

고객이 청소서비스 신청(CleanCall) 후 상태가 [호출중] 로 변경되고 청소담당자 배정이 완료되면 [호출확정] 로 변경이 되지만 ,
청소담당자 배정(CleanAssign)이 정상적이지 않으므로 [호출중]로 남게 됩니다.

< 고객 청소서비스 신청 >

![8 비동기 1 화면 캡처 2021-03-05 033544](https://user-images.githubusercontent.com/30484527/110013243-c594d880-7d64-11eb-97d5-3bbf180a0ed4.jpg)

<청소담당자 할당이 정상적이지 않아 호출중으로 남아있음>

![8 비동기 2 화면 캡처 2021-03-05 033544](https://user-images.githubusercontent.com/30484527/110013641-36d48b80-7d65-11eb-9cff-8178f5fb5b46.jpg)



## 정보 조회 / View 조회
고객은 청소서비스 신청, 처리되는 동안의 내용을 조회 할 수 있습니다.

![9 view 화면 캡처 2021-03-05 034859](https://user-images.githubusercontent.com/30484527/110014533-3ee0fb00-7d66-11eb-8451-91ade6a48a99.jpg)




## 소스 패키징

- 클라우드 배포를 위해서 다음과 같이 패키징 작업을 하였습니다.
```
cd gateway
mvn clean && mvn package
cd ..
cd cleancall
mvn clean && mvn package
cd ..
cd cleanmanage
mvn clean && mvn package
cd ..
cd cleanassign
mvn clean && mvn package
cd ..
```
<cleanassign>

![10 운영 package 화면 캡처 2021-03-05 040034](https://user-images.githubusercontent.com/30484527/110015629-6e443780-7d67-11eb-81ff-912bddc0b12f.jpg)


======================================================================================
# 클라우드 배포/운영 파이프라인

- 애저 클라우드에 배포하기 위해서 다음과 같이 주요 정보를 설정 하였습니다.

```
리소스 그룹명 : skuser13-rsrcgrp
클러스터 명   : skuser13-aks
레지스트리 명 : skuser13
```

- az login
  우선 az에 로그인 합니다.
```
```

- 토큰 가져오기
```
az aks get-credentials --resource-group skuser13-rsrcgrp --name skuser13-aks
```

- aks에 acr 붙이기
```
az aks update -n skuser13-aks -g skuser13-rsrcgrp --attach-acr skuser13

```
- 네임스페이스 만들기

```
kubectl create ns skuser13ns 
kubectl get ns
```
![11 ns 화면 캡처 2021-03-05 050724](https://user-images.githubusercontent.com/30484527/110023683-c7fd2f80-7d70-11eb-8c63-04ffa2c5a98f.jpg)

* 도커 이미지 만들고 레지스트리에 등록하기
```
cd cleancall
az acr build --registry skuser13 --image skuser13 .azurecr.io/taxicalleng:v1 .
az acr build --registry skuser13 --image skuser13.azurecr.io/taxicalleng:v2 .
cd ..
cd cleanmanage
az acr build --registry skuser13 --image skuser13.azurecr.io/cleanmanage:v1 .
cd ..
cd cleanassign
az acr build --registry skuser13 --image skuser13.io/cleanassign:v1 .
cd ..
cd gateway
az acr build --registry skuser13 --image skuser13.azurecr.io/gateway:v1 .
cd ..
cd customer
az acr build --registry skuser13 --image skuser13.azurecr.io/customer-policy-handler:v1 .
```
![12 이미지 등록 화면 캡처 2021-03-05 052032](https://user-images.githubusercontent.com/30484527/110025395-a1d88f00-7d72-11eb-94b2-951dd6c011f8.jpg)

-각 마이크로 서비스를 yml 파일을 사용하여 배포 합니다.

![13 배포 화면 캡처 2021-03-05 052410](https://user-images.githubusercontent.com/30484527/110025716-18758c80-7d73-11eb-8393-4bfbc00b4783.jpg)

- deployment.yml 및 sevice.yaml로 서비스 배포
```
cd ../../
cd gateway_eng/kubernetes
kubectl apply -f deployment.yml --namespace=skuser13ns
kubectl apply -f service.yaml --namespace=skuser13ns
```
<Deploy cleancall>

![14  배포2 화면 캡처 2021-03-05 053228](https://user-images.githubusercontent.com/30484527/110026698-50c99a80-7d74-11eb-816b-8344c71a1033.jpg)

<Deploy cleanassign>

![14  배포3 화면 캡처 2021-03-05 053228](https://user-images.githubusercontent.com/30484527/110026891-9f773480-7d74-11eb-8e5d-05299bf7774a.jpg)

- 서비스확인
```
kubectl get all -n skuser13ns
```
![14  배포 4 화면 캡처 2021-03-05 053228](https://user-images.githubusercontent.com/30484527/110029974-79ec2a00-7d78-11eb-8b0a-40f0536fe670.jpg)

## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현하였습니다.

- Hystrix 를 설정:

요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml
feign:
  hystrix:
    enabled: true

# To set thread isolation to SEMAPHORE
#hystrix:
#  command:
#    default:
#      execution:
#        isolation:
#          strategy: SEMAPHORE

hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```
![hystrix](https://user-images.githubusercontent.com/78134019/109652345-0218d680-7ba3-11eb-847b-708ba071c119.jpg)


부하테스트


* Siege 리소스 생성

```
kubectl run siege --image=apexacme/siege-nginx -n skuser13ns
```

* 실행

```
kubectl exec -it pod/siege-5459b87f86-q62hb -c siege -n skuser13ns -- /bin/bash
```

*부하 실행

```
siege -c190 -t60S -r10 -v --content-type "application/json" 'http://cleancall:8080/cleancalls POST {"tel": "0101231234"}'
```

- 부하 발생하여 CB가 발동하여 요청 실패처리하였고, 밀린 부하가 청소서비스 요청(cleancall) 서비스에서 처리되면서
  다시 cleancall에서 서비스를 받기 시작 합니다

![15 cb 화면 캡처 2021-03-05 074458](https://user-images.githubusercontent.com/30484527/110041152-351bbf80-7d87-11eb-86cf-3aeceeed05e3.jpg)

- report

![15 cb 2 화면 캡처 2021-03-05 074458](https://user-images.githubusercontent.com/30484527/110041496-8035d280-7d87-11eb-8201-d0eada382edd.jpg)


### 오토스케일 아웃

```
# autocale out 설정
 deployment.yml 설정
```

![auto1](https://user-images.githubusercontent.com/78134019/109794479-3ea70980-7c59-11eb-8d32-fbc039106c8c.jpg)

```
kubectl autoscale deploy cleanmanage --min=1 --max=10 --cpu-percent=15 -n skuser13ns

```
```
root@labs-1930058470:/home/project/personal/CLEAN-MALL2/cleanmanage/kubernetes# kubectl exec -it pod/siege-5459b87f86-q62hb -c siege -n skuser13ns -- /bin/bash
root@siege-5459b87f86-q62hb:/# siege -c200 -t120S -r10 -v --content-type "application/json" 'http://cleancall:8080/cleancalls POST {"tel": "0101231234"}'

```
![16 hpa 1 화면 캡처 2021-03-05 080745](https://user-images.githubusercontent.com/30484527/110043056-123eda80-7d8a-11eb-9116-b7efe4d54668.jpg)

- 오토스케일링에 대한 모니터링:
```
kubectl get deploy taxicall -w -n team03
```
![16 hpa 2 화면 캡처 2021-03-05 080745](https://user-images.githubusercontent.com/30484527/110043222-6053de00-7d8a-11eb-93d9-bfc89e6e49a9.jpg)



## 무정지 재배포

- deployment.yml에 readiness 옵션을 추가


![무정지 배포1](https://user-images.githubusercontent.com/78134019/109809110-45d71300-7c6b-11eb-955c-9b8a3b3db698.png)


- siege 실행
```
root@siege-5459b87f86-q62hb:/# siege -c100 -t120S -r10 -v --content-type "application/json" 'http://cleancall:8080/cleancalls POST {"tel": "0101231234"}'

```


- Availability: 100.00 % 확인

![19 무정지 재배포1 화면 캡처 2021-03-05 083850](https://user-images.githubusercontent.com/30484527/110045932-ce9a9f80-7d8e-11eb-90ae-7b237b84238d.jpg)

![19 무정지 재배포 2 화면 캡처 2021-03-05 083850](https://user-images.githubusercontent.com/30484527/110046173-e3773300-7d8e-11eb-8aab-94ea7ded6a5b.jpg)


