package app.mvc.session;

import java.util.HashSet;
import java.util.Set;

public class SessionSet {//싱글톤

	private static SessionSet ss = new SessionSet();
	private Set<Session> set; //중복안되고 순서없다! 처음에는 null. 로그인하는 순간 여기에 저장해줄거임
	
	private SessionSet() {
		set = new HashSet<>();
	}
	//싱글톤
	
	public static SessionSet getInstance() {//SessionSet.getInstance() 호출해서 SessionSet 리턴받는다.
		return ss;
	} // 싱글톤으로 만들어서 글로벌하게 사용
	// 프로젝트 생성하면서 생김
	
	
	/**
	 * 사용자 찾기
	 * */
	public Session get(String sessionId) {
		for(Session session : set) {
			if(session.getSessionId().equals(sessionId)) // 알맞은 사용자 세션을 찾아
			{
				return session;
			}
		}
		return null; // 해당 세션이 없으면
	}
	
	//세션 객체들 반환
		public Set<Session> getSet(){
			return set;
		}
	
		/**
		 * 로그인 된 사용자 추가
		 * */
		public void add(Session session) {
			set.add(session);
		}
		
	/**
	 * 사용자 제거 - 로그아웃
	 * */
	public void remove(Session session) {
		set.remove(session);// 세션 아이디로 세트에서 제거
	}
	
	
}
