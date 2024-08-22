package app.mvc.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import app.mvc.model.dto.Goods;
import app.mvc.model.dto.OrderLine;
import app.mvc.model.dto.Orders;
import app.mvc.util.DbManager;

public class OrderDAOImpl implements OrderDAO {
	
	GoodsDAO goodsDao = new GoodsDAOImpl();
    /**
     * 주문하기 
     *        1) orders테이블에 insert 하나의 주문 내역
     *        2) order_line테이블에 insert 주문 내역들
     *        3) goods의 재고량 감소(update)
     * */
	@Override
	public int orderInsert(Orders order)throws SQLException {
		// 하나의 주문에 여러 상품들을 담아
		 Connection con=null;
		  PreparedStatement ps=null;
		  String sql="INSERT INTO ORDERS(ORDER_ID, ORDER_DATE,USER_ID, ADDRESS, TOTAL_AMOUNT)" + 
		  		"  VALUES(ORDER_ID_SEQ.NEXTVAL ,sysdate,?,?, ?)";
		  int result=0;
		 try {
			
		   con = DbManager.getConnection();
		   con.setAutoCommit(false);//자동커밋해지
		   
		   ps = con.prepareStatement(sql);
		   ps.setString(1, order.getUserId()); // 받아온 객체들에서 값을 얻어서 디비에 넣어
		   ps.setString(2, order.getAddress());
		   
		   ps.setInt(3, this.getTotalAmount(order) );//총구매금액구하는 메소드 호출
		   
		   result = ps.executeUpdate();
		   if(result==0)
		   {
			   con.rollback();
			   throw new SQLException("주문 실패...");
		   }
		   else {
			   int re [] = this.orderLineInsert(con, order); //주문상세 등록하기 , 커넥션을 전달
			   for(int i : re)  // 결과들을 돌면섯 하나라도 문제가 있다면 롤백할 수 있도록 함
			   {
				   if(i != 1)
				   {//
					   con.rollback();
					   throw new SQLException("주문 할수 없습니다....");
				   }
			   }
			   
			   //주문수량만큼 재고량 감소하기
			   this.decrementStock(con, order.getOrderLineList());
			   
			   con.commit();
		   }
		   
      }finally
		 {
    	  con.commit();
      	DbManager.close(con, ps , null);
      }
		
		return result;
	}
	
	/**
	 * 주문상세 등록하기 
	 * */
	public int[] orderLineInsert(Connection con  , Orders order) throws SQLException{
		
		  PreparedStatement ps=null;
		  String sql="insert into order_line(order_line_id,order_id, goods_id,unit_price, qty, amount)" + 
		  		"  values(ORDER_LINE_ID_SEQ.nextval ,ORDER_ID_SEQ.currval , ?, ? , ? , ? )";
		  int result [] =null;
		 try {
			 ps = con.prepareStatement(sql);
			 
		  for( OrderLine orderline : order.getOrderLineList() )  // 주문된 order들을 모두 insert 해줌
		  {
			 Goods goods = goodsDao.goodsSelectBygoodsId(orderline.getGoodsId());
			  
			   ps.setString(1, orderline.getGoodsId()); //상품코드
			   ps.setInt(2, goods.getGoodsPrice());//가격
			   ps.setInt(3, orderline.getQty());//수량
			   ps.setInt(4,  goods.getGoodsPrice()*orderline.getQty());//총구매금액
			   
			   //ps.executeUpdate(); 이걸로 하면 계속 각 orderline마다 쿼리를 던짐
			   
			   ps.addBatch(); //일괄처리할 작업에 추가,
			   ps.clearParameters();
		  }
		  
		  result = ps.executeBatch();//일괄처리, 모든 orderline은 묶어서 전송. 모든 반복문들이 끝나면 쿼리문들이 모아짐
			 // 여러 개를 배치로 보내기 때문에 각각의 쿼리문들에 대한 결과들이 배열에 들어감.그래서 배열로 리턴함
		  
		   
    }finally {
    	DbManager.close(null, ps , null);
    }
		
		return result;
		
	}
	
	/**
	 * 상품으로 재고량 감소시키키
	 * */
	public int[] decrementStock(Connection con , List<OrderLine> orderLineList)throws SQLException {
		 PreparedStatement ps=null;
		  String sql="update goods set stock = stock-? where goods_id=?";
		  int result [] =null;
		 try {
		  ps = con.prepareStatement(sql);
		  for( OrderLine orderline : orderLineList ) {
			   ps.setInt(1, orderline.getQty());
			   ps.setString(2, orderline.getGoodsId());
			   
			   ps.addBatch(); //일괄처리할 작업에 추가
			   ps.clearParameters();
		  }
		  
		  result = ps.executeBatch();//일괄처리
		  
		 }
		 finally
		 {
			 DbManager.close(null, ps, null);
		 }
		
		return result;
	}
	
	/**
	 * 상품 총구매금액 구하기
	 * */
	public int getTotalAmount(Orders order) throws SQLException { //상품가격 , 수량
		List<OrderLine> orderLineList= order.getOrderLineList();
	    int total=0;
		for(OrderLine line : orderLineList) {
			Goods goods =goodsDao.goodsSelectBygoodsId(line.getGoodsId());//상품에해당하는 정보 검색
			
			if(goods==null)throw new SQLException("상품번호 오류입니다.... 주문 실패..");
			else if(goods.getStock() <  line.getQty())throw new SQLException("재고량 부족입니다...");
			
	    	total += line.getQty() * goods.getGoodsPrice() ; //주문수량 * 가격
	    }
		
		return total;
	}
	
	/**
	 * 주문내역 보기
	 * */
	public List<Orders> selectOrdersByUserId(String userId)throws SQLException{
		/*
		orders도 리스트이고 orderline도 리스트이니까 둘이 조인해서 가지고 오기 어려움->둘이 분리함
		orders안에 orderline 있음
		 */
		Connection con=null;
		  PreparedStatement ps=null;
		  ResultSet rs=null;
		  List<Orders> list = new ArrayList<>();
		 try {
		   con = DbManager.getConnection();
		   ps= con.prepareStatement("select * from orders where user_id=? order by order_id desc");
		   ps.setString(1, userId);
	       rs = ps.executeQuery(); 
	        
	        while(rs.next()) {
	        	Orders orders  = new Orders(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5));
	        	
	        	//주문번호에 해당하는 상세정보 가져오기, orders안에 orderline 있음
	        	List<OrderLine> orderLineList = this.selectOrderLine(orders.getOrderId());//메소드 호출
	        	
	        	orders.setOrderLineList(orderLineList);
	        	
	        	list.add(orders);
	        }
    }finally {
    	DbManager.close(con, ps, rs);
    }
		return list;
		
	}
	
	/**
	 * 주문번호에 해당하는 주문상세 가져오기
	 * */
	public List<OrderLine> selectOrderLine(int orderId)throws SQLException{
		Connection con=null;
		  PreparedStatement ps=null;
		  ResultSet rs=null;
		  List<OrderLine> list = new ArrayList<>();
		 try {
		   con = DbManager.getConnection();
		   ps= con.prepareStatement("select * from order_line where  order_id=?");
		   ps.setInt(1, orderId);
	       rs = ps.executeQuery(); 
	        
	        while(rs.next()) {
	        	OrderLine orderLine  = new OrderLine(rs.getInt(1), rs.getInt(2), 
	        			rs.getString(3), rs.getInt(4), rs.getInt(5), rs.getInt(6));
	        	list.add(orderLine);
	        }
    }finally {
    	DbManager.close(con, ps, rs);
    }
		return list;
		
	}
}







