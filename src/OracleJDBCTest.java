import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class OracleJDBCTest {
    public static void main(String[] args) {
        // JDBC URL 및 사용자 정보
        String driverName = "oracle.jdbc.driver.OracleDriver";
        String url = "jdbc:oracle:thin:@cloudb_high?TNS_ADMIN=C:/wallet/Wallet_CLOUDB";
        String userName = "admin";
        String userPass = "Jmkh42512456!";

        Connection connection = null;
        Statement statement = null;

        try {
            // JDBC 드라이버 로드
            System.out.println("JDBC 드라이버 로드 중...");
            Class.forName(driverName);

            // 데이터베이스 연결
            System.out.println("데이터베이스에 연결 중...");
            connection = DriverManager.getConnection(url, userName, userPass);
            System.out.println("데이터베이스 연결 성공!");

            // SQL 문 실행
            statement = connection.createStatement();

            // 테이블 생성 SQL 명령어 실행
            String createTableSQL = "CREATE TABLE test (name VARCHAR2(20))";
            statement.executeUpdate(createTableSQL);
            System.out.println("테이블 'test'가 성공적으로 생성되었습니다.");

            // 데이터 삽입 SQL 명령어 실행
            String insertSQL = "INSERT INTO test (name) VALUES ('John Doe')";
            statement.executeUpdate(insertSQL);
            System.out.println("데이터가 'test' 테이블에 성공적으로 삽입되었습니다.");

        } catch (ClassNotFoundException e) {
            System.out.println("JDBC 드라이버를 찾을 수 없습니다: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            System.out.println("SQL 오류: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("예기치 않은 오류 발생: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 리소스 정리
            try {
                if (statement != null) statement.close();
                if (connection != null) connection.close();
                System.out.println("연결이 정상적으로 종료되었습니다.");
            } catch (SQLException e) {
                System.out.println("리소스 정리 중 오류 발생: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
