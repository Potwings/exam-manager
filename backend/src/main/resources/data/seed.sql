-- ============================================================
-- 백엔드 개발자 필기시험 시드 데이터
-- 사용법: 초기 데이터가 필요할 때 수동으로 실행 (자동 실행 아님)
--   mysql -u root -p exam_scorer < seed.sql
-- ============================================================

-- 시험 생성
INSERT INTO exams (title, deleted, active, created_at)
VALUES ('백엔드 개발자 필기시험', false, true, NOW());

SET @exam_id = LAST_INSERT_ID();

-- ===== 문제 (Q1~Q14) =====

INSERT INTO problems (problem_number, content, exam_id) VALUES
(1, 'OOP(객체 지향 프로그래밍)의 4대 특징을 설명하세요.', @exam_id);
SET @p1 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(2, 'Overloading과 Overriding의 차이점을 설명하세요.', @exam_id);
SET @p2 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(3, 'JAVA 접근제어자의 종류와 각각의 제한 범위에 대하여 설명하세요.', @exam_id);
SET @p3 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(4, '자료형의 기본타입(primitive type)과 래퍼 클래스(wrapper class)에 대해 설명하세요.', @exam_id);
SET @p4 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(5, 'Process와 Thread에 대해 설명하세요.', @exam_id);
SET @p5 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(6, 'GET, POST에 대해 설명하세요.', @exam_id);
SET @p6 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(7, 'Cookie와 Session에 대해 설명하세요.', @exam_id);
SET @p7 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(8, '가비지 컬렉션(Garbage Collection)에 대해 설명하세요.', @exam_id);
SET @p8 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(9, '아래의 코드의 실행 결과를 작성하세요.\npublic static void main (String[] argv) {\n    String number;\n    System.out.println(toInt(number));\n    number = \"9900\";\n    System.out.println(toInt(number));\n    number = \"10\";\n    System.out.println(toInt(number));\n}\npublic static int toInt(String strValue) {\n    if ( strValue == null || strValue.length() == 0 ) {\n        throw new IllegalArgumentException();\n    }\n    int intValue = 0;\n    try {\n        intValue = Integer.parseInt(strValue);\n    } catch (Exception e) {\n        intValue = 0;\n    }\n    return intValue;\n}', @exam_id);
SET @p9 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(10, '아래의 코드의 실행 결과를 작성하세요.\npublic static void main(String[] args) {\n    String s1 = new String(\"abc\");\n    String s2 = new String(\"xyz\");\n    if (s1 == s2) {\n        System.out.println(\"s1 == s2 is true\");\n    } else {\n        System.out.println(\"s1 == s2 is false\");\n    }\n    String s3 = s1;\n    if (s1 == s3) {\n        System.out.println(\"s1 == s3 is true\");\n    } else {\n        System.out.println(\"s1 == s3 is false\");\n    }\n    if (s1.equals(s3)) {\n        System.out.println(\"s1 == s3 is true\");\n    } else {\n        System.out.println(\"s1 == s3 is false\");\n    }\n    s1 = new String(\"abc\");\n    s2 = new String(\"abc\");\n    if (s1 == s2) {\n        System.out.println(\"s1 == s2 is true\");\n    } else {\n        System.out.println(\"s1 == s2 is false\");\n    }\n}', @exam_id);
SET @p10 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(11, '아래의 javascript코드의 실행 결과를 작성하세요.\nvar x = 10;\nconsole.log(x);\nfunction test () {\n    var x = 20;\n    console.log(x);\n    if (x > 10) {\n        let x = 30;\n        console.log(x);\n    }\n    console.log(x);\n}\ntest();\nconsole.log(x);', @exam_id);
SET @p11 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(12, '아래의 코드의 실행 결과는 undefined입니다. 이유를 설명하세요.\nasync function foo(){\n    return ''test'';\n}\nvar result = foo();\nconsole.log(result);', @exam_id);
SET @p12 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(13, '아래 조직 정보 테이블이 있습니다. 각 하위 질문에 맞는 답변을 작성하세요.\n\n[사원정보]\n사원번호 | 이름     | 직급코드 | 부서코드\n1       | 홍이사   | 6       | 1\n2       | 최사원   | 1       | 1\n3       | 나사장   | 7       | 0\n4       | 김차장   | 4       | 1\n5       | 박대리   | 2       | 1\n6       | 오부장   | 5       | 2\n7       | 조사원   | 1       | 1\n8       | 고이사   | 6       | 2\n9       | 이과장   | 3       | 2\n\n[직급정보]\n직급코드 | 직급명 | 정렬\n1       | 사원   | 7\n2       | 대리   | 6\n3       | 과장   | 5\n4       | 차장   | 4\n5       | 부장   | 3\n6       | 이사   | 2\n7       | 사장   | 1\n\n[부서정보]\n부서코드 | 부서명\n1       | 개발팀\n2       | 영업팀\n\nA. 사원정보를 추가하는 쿼리를 작성하세요. 단 사원번호는 자동 증가합니다.\n   이름 : 정사원, 부서명 : 개발팀, 직급명 : 사원\n\nB. 사원정보를 수정하는 쿼리를 작성하세요.\n   최사원이 영업팀으로 이동\n\nC. 사원 정보를 삭제하는 쿼리를 작성하세요.\n   사원번호가 9인 사원이 퇴사\n\nD. 사용정보를 출력하는 쿼리를 작성하세요.\n   직급명이 사원인 사용자들의 이름과 부서명 출력\n\nE. 제약사항을 준수하여 아래의 결과를 출력할 수 있는 쿼리를 작성하세요.\n   제약사항:\n   1. 사원은 출력하지 않음\n   2. 사원정보의 부서코드가 부서정보에 존재하지 않는 경우 빈 칸으로 출력함\n   3. 결과는 직급정보의 정렬 순서와 이름으로 오름차순 정렬함\n\n   [기대 결과]\n   사원번호 | 이름   | 부서명   | 직급명\n   3       | 나사장 |         | 사장\n   8       | 고이사 | 영업팀   | 이사\n   1       | 홍이사 | 개발팀   | 이사\n   6       | 오부장 | 영업팀   | 부장\n   4       | 김차장 | 개발팀   | 차장\n   9       | 이과장 | 영업팀   | 과장\n   5       | 박대리 | 개발팀   | 대리', @exam_id);
SET @p13 = LAST_INSERT_ID();

INSERT INTO problems (problem_number, content, exam_id) VALUES
(14, '아래의 조건대로 함수를 작성하세요.\n1. 문자열의 짝수번째 알파벳은 대문자로, 홀수번째 알파벳은 소문자로 바꾼 문자열을 리턴하는 함수를 완성하세요.\n2. 입출력 예\n   - input : hello java world → output : HeLlO JaVa wOrLd', @exam_id);
SET @p14 = LAST_INSERT_ID();

-- ===== 채점 기준 (A1~A14) =====

INSERT INTO answers (content, score, problem_id) VALUES
('다음 4가지 특징을 각각 "설명"해야 합니다 (각 2점, 총 8점):\n1. 추상화: 공통적인 속성이나 기능을 묶어 이름을 붙이는 것 → 설명 포함 시 2점\n2. 캡슐화: 데이터와 메서드를 하나로 묶고 외부에서 접근을 제한 → 설명 포함 시 2점\n3. 상속성: 부모 클래스의 속성과 메서드를 자식 클래스가 물려받는 것 → 설명 포함 시 2점\n4. 다형성: 같은 인터페이스가 다양한 형태로 동작하는 것 → 설명 포함 시 2점\n[필수 키워드] "추상화", "캡슐화", "상속성", "다형성" — 이 4가지 용어가 반드시 등장해야 하며, 각각에 대한 설명이 있어야 만점.\n[감점 규칙] 이름만 나열하고 설명이 없으면 각 항목당 1점만 부여 (최대 4점).\n설명 없이 "추상화, 캡슐화, 상속, 다형성"만 쓴 답안은 절대 만점(8점)이 아닌 4점 처리.', 8, @p1);

INSERT INTO answers (content, score, problem_id) VALUES
('Overloading과 Overriding 각각 "설명"해야 합니다 (각 3점, 총 6점):\n1. Overloading: 같은 이름의 메서드를 여러 개 정의, 매개변수의 타입이나 개수가 달라야 함 → 설명 포함 시 3점\n   - 개념만 언급하고 설명 없으면 1점\n   - 매개변수 조건까지 설명하면 3점\n2. Overriding: 부모 클래스의 메서드를 자식 클래스에서 상속받아서 재정의, 매개변수의 타입이나 개수가 같아야 함 → 설명 포함 시 3점\n   - 개념만 언급하고 설명 없으면 1점\n   - 조건까지 설명하면 3점\n[필수 키워드]\n- Overloading: "같은 이름의 메서드를", "매개변수의 타입이나 개수가 달라야"\n- Overriding: "상속", "재정의", "매개변수의 타입이나 개수가 같아야"\n[감점 규칙] "Overloading은 오버로딩이고 Overriding은 오버라이딩이다" 같은 단순 번역은 각 0점.\n용어만 나열하면 절대 만점 불가, 최대 2점.', 6, @p2);

INSERT INTO answers (content, score, problem_id) VALUES
('JAVA 접근제어자 3가지 이상의 종류와 "접근 범위를 설명"해야 합니다 (각 2점, 총 6점):\n1. private: 같은 클래스 내에서만 접근 가능 → 접근 범위 설명 시 2점\n2. public: 접근 제한 없음, 어디서든 접근 가능 → 접근 범위 설명 시 2점\n3. protected: 같은 패키지 내에서 접근 가능 → 접근 범위 설명 시 2점\n- default(package-private)를 추가로 언급하면 가산점 없이 인정\n[필수 키워드]\n- private: "같은 클래스 내에서"\n- public: "접근 제한이 없음" 또는 "같은 프로젝트 내에서"\n- protected: "같은 패키지 내에서"\n[감점 규칙] 접근 범위 설명 없이 이름만 나열하면 각 1점만 부여.\n"private, public, protected"만 쓰면 절대 만점 불가, 최대 3점.', 6, @p3);

INSERT INTO answers (content, score, problem_id) VALUES
('기본 타입과 래퍼 클래스 각각 "설명"해야 합니다 (각 3점, 총 6점):\n1. 기본 타입(primitive type): 메모리에 저장되어 있는 값(value)을 복사하여 보낸다, int/boolean 등 → 설명 포함 시 3점\n   - 종류만 나열하면 1점\n   - 값 저장 방식(값 복사 등)까지 설명하면 3점\n2. 래퍼 클래스(wrapper class): 기본 타입을 객체로 감싼 것, Integer/Boolean 등 → 설명 포함 시 3점\n   - 종류만 나열하면 1점\n   - "객체"로 감싸는 개념이나 용도까지 설명하면 3점\n[필수 키워드]\n- 기본 타입: "메모리에 저장되어 있는 값(value)" 또는 "값을 직접 저장"\n- 래퍼 클래스: "객체" (객체로 감싼다는 개념이 핵심)\n[감점 규칙] "int, boolean"이나 "Integer, Boolean" 같이 종류만 나열하면 각 1점.\n설명 없이 나열만 하면 절대 만점 불가, 최대 2점.', 6, @p4);

INSERT INTO answers (content, score, problem_id) VALUES
('Process와 Thread 각각 "설명"해야 합니다 (각 3점, 총 6점):\n1. Process: 메모리에 올라와 실행되고 있는 프로그램, 운영체제로부터 시스템 자원을 할당받는 작업의 단위 → 설명 포함 시 3점\n   - "실행 중인 프로그램"이라는 개념만 언급하면 1점\n   - "작업의 단위" 등 추가 설명 시 3점\n2. Thread: 프로세스 내에서 실제로 작업을 수행하는 주체, 프로세스가 할당받은 자원을 이용하는 실행의 단위 → 설명 포함 시 3점\n   - "실행 단위"라는 개념만 언급하면 1점\n   - "실제로 작업을 수행" 등 추가 설명 시 3점\n[필수 키워드]\n- Process: "메모리에 올라와 실행되고 있는 프로그램", "실행 중인 프로그램", "작업의 단위"\n- Thread: "실제로 작업을 수행", "실행의 단위"\n[감점 규칙] "프로세스는 프로그램이고 쓰레드는 실행 단위이다" 같은 한 줄 답변은 각 1점.\n설명 없이 정의만 쓰면 절대 만점 불가, 최대 2점.', 6, @p5);

INSERT INTO answers (content, score, problem_id) VALUES
('GET과 POST 각각 "설명"해야 합니다 (각 3점, 총 6점):\n1. GET: URL, Header에 데이터가 포함, 길이 제한 있음, 보안에 취약 → 설명 포함 시 3점\n   - 핵심 특징 1가지만 언급하면 1점\n   - "URL, Header에 데이터가 포함" + "길이 제한" 모두 설명 시 3점\n2. POST: 데이터가 Body에 포함, URL에 데이터가 노출되지 않음 → 설명 포함 시 3점\n   - 핵심 특징 1가지만 언급하면 1점\n   - "URL에 데이터가 노출되지 않음" 포함 시 3점\n[필수 키워드]\n- GET: "URL, Header에 데이터가 포함", "길이 제한"\n- POST: "URL에 데이터가 노출되지 않음"\n[감점 규칙] "GET은 조회, POST는 전송"처럼 한 단어 수준 답변은 각 1점.\n구체적 설명 없이 용도만 쓰면 절대 만점 불가, 최대 2점.', 6, @p6);

INSERT INTO answers (content, score, problem_id) VALUES
('Cookie와 Session 각각 "설명"해야 합니다 (각 3점, 총 6점):\n1. Cookie: 클라이언트 로컬에 저장되는 키와 값이 들어있는 데이터 파일 → 설명 포함 시 3점\n   - "클라이언트 로컬에 저장"만 언급하면 2점\n   - 키-값 구조, 유효기간 등 추가 설명 시 3점\n   - 저장 위치 언급 없이 이름만 쓰면 0점\n2. Session: 브라우저를 종료할 때까지 유지되는 상태, 서버에 저장 → 설명 포함 시 3점\n   - "서버에 저장"만 언급하면 2점\n   - "브라우저를 종료할 때까지 유지되는 상태" 추가 설명 시 3점\n   - 저장 위치 언급 없이 이름만 쓰면 0점\n[필수 키워드]\n- Cookie: "클라이언트 로컬에 저장"\n- Session: "브라우저를 종료할 때까지 유지되는 상태", "서버에 저장"\n[감점 규칙] "쿠키는 쿠키이고 세션은 세션이다" 같은 답변은 0점.\n저장 위치(클라이언트/서버) 설명 없으면 절대 만점 불가.', 6, @p7);

INSERT INTO answers (content, score, problem_id) VALUES
('가비지 컬렉션을 "설명"해야 합니다 (총 6점):\n- "메모리관리" 개념을 언급 시 2점\n- "객체 해제" (더 이상 참조되지 않는 객체를 해제) 언급 시 2점\n- "메모리 누수 방지" 목적 언급 시 2점\n[필수 키워드] "메모리관리", "객체 해제", "메모리 누수 방지" — 이 3가지가 핵심 키워드.\n[감점 규칙]\n- "메모리 관리"만 쓰면 2점\n- "가비지 컬렉션은 메모리를 관리하는 것이다" 수준은 2점\n- 3가지 키워드 중 2가지만 설명하면 4점, 1가지만 설명하면 2점\n- 키워드만 나열하고 설명 없으면 최대 3점, 절대 만점 불가', 6, @p8);

INSERT INTO answers (content, score, problem_id) VALUES
('코드 실행 결과 채점 (총 5점):\n- 오류가 발생한다는 것을 언급하면 2점\n- 이유: "number가 초기화되지 않은" 상태로 매개변수로 사용할 수 없음 → 이유 설명 시 추가 3점\n[필수 키워드] "number가 초기화되지 않은" — 이 표현이 이유 설명의 핵심.\n[감점 규칙]\n- "오류 발생"만 쓰고 이유 없으면 2점, 절대 만점 불가\n- 오류 발생 + "초기화되지 않은" 이유까지 설명하면 5점\n- 실행 결과를 출력값으로 작성한 경우(9900, 10 등) 0점', 5, @p9);

INSERT INTO answers (content, score, problem_id) VALUES
('코드 실행 결과 채점 (총 5점):\n정답 출력 순서 (순서대로 정확히 작성해야 함):\n1번째 출력: s1 == s2 is false → 정확히 맞으면 1점\n2번째 출력: s1 == s3 is true → 정확히 맞으면 1점\n3번째 출력: s1 == s3 is true → 정확히 맞으면 1점 (equals 비교)\n4번째 출력: s1 == s2 is false → 정확히 맞으면 2점 (new String이므로 참조 다름)\n[감점 규칙]\n- 각 출력 결과가 정확해야 해당 점수 부여\n- 순서가 틀리면 해당 항목 0점\n- true/false만 쓰고 전체 문장이 없어도 순서가 맞으면 인정', 5, @p10);

INSERT INTO answers (content, score, problem_id) VALUES
('JavaScript 코드 실행 결과 채점 (총 5점):\n정답 출력 순서 (순서대로 정확히 작성해야 함):\n1번째: 10\n2번째: 20\n3번째: 30\n4번째: 20\n5번째: 10\n[필수 조건] "출력 결과를 전부 맞춰야" 점수 부여.\n[감점 규칙]\n- 5개 출력 결과를 순서대로 모두 정확히 맞추면 5점\n- 하나라도 틀리면 0점 (부분 점수 없음)\n- var와 let의 스코프 차이를 이해해야 정답을 맞출 수 있음', 5, @p11);

INSERT INTO answers (content, score, problem_id) VALUES
('undefined 출력 이유를 "설명"해야 합니다 (총 3점):\n- "비동기"를 언급하면 기본 점수 부여\n- async 함수는 Promise를 반환한다는 것을 언급하면 2점\n- await 없이 호출하여 Promise 객체가 출력됨을 설명하면 추가 1점\n[필수 키워드] "비동기" — 이 키워드가 핵심. 추가로 Promise 반환을 설명하면 만점.\n[감점 규칙]\n- "비동기" 키워드만 언급하고 구체적 설명 없으면 1점\n- Promise 반환을 언급하지 않으면 절대 만점 불가\n- "비동기라서" 한마디만 쓰면 1점', 3, @p12);

INSERT INTO answers (content, score, problem_id) VALUES
('SQL 문제 채점 (총 22점, 5개 소문제). 각 소문제별로 부분 점수를 독립적으로 채점하세요:\n\nA. INSERT 쿼리 (3점):\n모범답안: INSERT INTO 사원정보(이름, 직급코드, 부서코드) VALUES (''정사원'', 1, 1);\n[필수 키워드] "INSERT INTO", "VALUES", "사원번호는 자동 증가"(사원번호 컬럼 제외)\n- INSERT INTO 구문 사용 (1점)\n- 올바른 컬럼과 값 매핑 (1점)\n- 사원번호 제외 (자동 증가) (1점)\n[감점] INSERT 키워드 없으면 0점. 컬럼-값 매핑 틀리면 해당 부분 감점.\n\nB. UPDATE 쿼리 (2점):\n모범답안: UPDATE 사원정보 SET 부서코드 = 2 WHERE 사원번호 = 2;\n[필수 키워드] "UPDATE", "SET", "WHERE"\n- UPDATE SET 구문 사용 (1점)\n- 올바른 WHERE 조건 (1점)\n[감점] UPDATE 키워드 없으면 0점. WHERE 없으면 1점.\n\nC. DELETE 쿼리 (2점):\n모범답안: DELETE FROM 사원정보 WHERE 사원번호 = 9;\n[필수 키워드] "DELETE FROM", "WHERE"\n- DELETE FROM 구문 사용 (1점)\n- 올바른 WHERE 조건 (1점)\n[감점] DELETE 키워드 없으면 0점. WHERE 없으면 1점.\n\nD. SELECT + JOIN 쿼리 (5점):\n모범답안: SELECT a.이름, b.부서명 FROM 사원정보 a JOIN 부서정보 b ON a.부서코드 = b.부서코드 JOIN 직급정보 c ON a.직급코드 = c.직급코드 WHERE c.직급명 = ''사원'';\n[필수 키워드] "SELECT", "FROM", "JOIN", "WHERE"\n- SELECT 구문 (1점)\n- 적절한 JOIN 사용 (2점)\n- WHERE 조건 정확 (2점)\n[감점] JOIN 없이 서브쿼리 사용 시 로직 맞으면 부분 점수 인정. SELECT만 쓰고 JOIN 없으면 최대 1점.\n\nE. 복합 쿼리 (10점):\n모범답안: SELECT s.사원번호, s.이름, b.부서명, j.직급명 FROM 사원정보 s INNER JOIN 직급정보 j ON s.직급코드 = j.직급코드 LEFT OUTER JOIN 부서정보 b ON s.부서코드 = b.부서코드 WHERE s.직급코드 > 1 ORDER BY j.정렬 ASC, s.이름 ASC;\n- INNER JOIN 직급정보 사용 (2점)\n- LEFT OUTER JOIN 부서정보 사용 (부서코드가 없는 경우 빈칸 출력 위해 필수) (3점)\n- 사원 제외 조건 (WHERE 직급코드 > 1 또는 직급명 != ''사원'') (2점)\n- ORDER BY 정렬 조건 정확 (3점)\n[감점] LEFT JOIN 대신 INNER JOIN만 사용하면 3점 감점. ORDER BY 없으면 3점 감점. 쿼리 미작성 시 0점.', 22, @p13);

INSERT INTO answers (content, score, problem_id) VALUES
('함수 작성 채점 (총 10점):\n요구사항: 문자열의 짝수번째(0-indexed) 알파벳은 대문자, 홀수번째는 소문자로 변환\n입력: "hello java world" → 출력: "HeLlO JaVa wOrLd"\n\n채점 기준:\n- 올바른 함수 구조 (메서드 시그니처, 반환값) → 2점\n- 문자열 순회 로직 (반복문 사용) → 2점\n- 짝수/홀수 인덱스 판별 및 대소문자 변환 로직 → 3점\n- 최종 결과가 정확 (HeLlO JaVa wOrLd) → 3점\n- 언어는 Java, JavaScript, Python 등 어떤 언어든 허용\n[필수 조건] "결과가 맞으면" 만점. 코드의 최종 출력이 "HeLlO JaVa wOrLd"와 일치해야 결과 점수 부여.\n[감점 규칙]\n- 코드 없이 결과만 쓰면 최대 3점 (결과 정확 시)\n- 함수 구조 없이 로직만 서술하면 최대 5점\n- 코드가 있지만 결과가 틀리면 로직 부분 점수만 부여 (최대 7점)\n- 결과가 정확하면 코드 스타일과 무관하게 만점', 10, @p14);
