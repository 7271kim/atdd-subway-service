package nextstep.subway.line.acceptance;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static nextstep.subway.station.StationAcceptanceTest.지하철_역_목록에_포함되지_않음;
import static nextstep.subway.station.StationAcceptanceTest.지하철역_목록_조회_요청;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("지하철 노선 관련 기능")
public class LineAcceptanceTest extends AcceptanceTest {
    private StationResponse 강남역;
    private StationResponse 광교역;
    private LineRequest lineRequest1;
    private LineRequest lineRequest2;

    @BeforeEach
    public void setUp() {
        super.setUp();

        // given
        강남역 = StationAcceptanceTest.지하철역_등록되어_있음("강남역").as(StationResponse.class);
        광교역 = StationAcceptanceTest.지하철역_등록되어_있음("광교역").as(StationResponse.class);

        lineRequest1 = new LineRequest("신분당선", "bg-red-600", 강남역.getId(), 광교역.getId(), 10);
        lineRequest2 = new LineRequest("구신분당선", "bg-red-600", 강남역.getId(), 광교역.getId(), 15);
    }

    @DisplayName("시나리오1: 지하철 노선을 관리한다.")
    @Test
    void manageLineTest() {
        LineRequest changeRequest = new LineRequest("changedName", lineRequest1.getColor(),
                lineRequest1.getUpStationId(), lineRequest1.getDownStationId(), lineRequest1.getDistance());

        // when
        ExtractableResponse<Response> createResponse = 지하철_노선_생성_요청(lineRequest1);

        // then
        지하철_노선_생성됨(createResponse);

        // when
        ExtractableResponse<Response> response = 지하철_노선_목록_조회_요청();

        // then
        지하철_노선_목록_응답됨(response);
        지하철_노선_목록_포함됨(response, Collections.singletonList(createResponse));

        // when
        ExtractableResponse<Response> modifyResponse = 지하철_노선_수정_요청(createResponse, changeRequest);

        // then
        지하철_노선_수정됨(modifyResponse);

        // when
        ExtractableResponse<Response> removeResponse = 지하철_노선_제거_요청(createResponse);

        // then
        지하철_노선_삭제됨(removeResponse);
    }

    @DisplayName("시나리오2: 서로 겹치는 환승역이 있는 지하철 노선을 등록한다.")
    @Test
    void addLineWithDuplicatedStation() {
        // given
        ExtractableResponse<Response> createResponse = 지하철_노선_생성되어_있음(lineRequest1);

        // when
        ExtractableResponse<Response> createResponse2 = 지하철_노선_생성되어_있음(lineRequest2);

        // then
        두_노선에_겹치는_역이_존재함(createResponse, createResponse2);
    }

    @DisplayName("시나리오3: 실수로 같은 지하철 노선을 두번 등록한다.")
    @Test
    void addLineTwiceTest() {
        // given
        지하철_노선_생성되어_있음(lineRequest1);

        // when
        ExtractableResponse<Response> createSecondResponse = 지하철_노선_생성_요청(lineRequest1);

        // then
        지하철_노선_생성_실패됨(createSecondResponse);
    }

    @DisplayName("시나리오4: 실수로 종점역을 빠뜨린 채로 지하철 노선을 등록 요청한다.")
    @Test
    void addLineWithoutEndStations() {
        LineRequest mistakeRequest = new LineRequest("종점역이 없는 노선", "종점역이 없는 색", null, null, 10);

        // when
        ExtractableResponse<Response> createResponse = 지하철_노선_생성_요청(mistakeRequest);

        // then
        지하철_노선_생성_실패됨(createResponse);
    }

    @DisplayName("시나리오5: 실수로 존재하지 않는 지하철역으로 지하철 노선 등록 요청한다.")
    @Test
    void addLineWithNotExistStation() {
        Long notExistStationId1 = 100L;
        Long notExistStationId2 = 1000L;

        // given
        지하철_역_목록에_포함되지_않음(notExistStationId1, notExistStationId2);

        // when
        ExtractableResponse<Response> response = 지하철_노선_생성_요청(new LineRequest("새노선", "좋은색", notExistStationId1, notExistStationId2, 3));

        // then
        지하철_노선_생성_실패됨(response);
    }

    @DisplayName("시나리오6: 실수로 등록한 적 없는 지하철 노선을 수정하거나 삭제한다.")
    @Test
    void deleteOrModifyNotExistLineTest() {
        Long notExistLineId = 1000L;

        // when
        ExtractableResponse<Response> modifyResponse = 지하철_노선_수정_요청(notExistLineId, lineRequest1);

        // then
        지하철_노선_수정_실패됨(modifyResponse);

        // when
        ExtractableResponse<Response> deleteResponse = 지하철_노선_제거_요청(notExistLineId);

        // then
        지하철_노선_수정_실패됨(deleteResponse);
    }

    public static ExtractableResponse<Response> 지하철_노선_등록되어_있음(LineRequest params) {
        return 지하철_노선_생성_요청(params);
    }

    public static ExtractableResponse<Response> 지하철_노선_생성_요청(LineRequest params) {
        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().post("/lines")
                .then().log().all().
                        extract();
    }

    public static ExtractableResponse<Response> 지하철_노선_목록_조회_요청() {
        return 지하철_노선_목록_조회_요청("/lines");
    }

    public static ExtractableResponse<Response> 지하철_노선_목록_조회_요청(ExtractableResponse<Response> response) {
        String uri = response.header("Location");

        return 지하철_노선_목록_조회_요청(uri);
    }

    private static ExtractableResponse<Response> 지하철_노선_목록_조회_요청(String uri) {
        return RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get(uri)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 지하철_노선_조회_요청(LineResponse response) {
        return RestAssured
                .given().log().all()
                .accept(MediaType.APPLICATION_JSON_VALUE)
                .when().get("/lines/{lineId}", response.getId())
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 지하철_노선_수정_요청(ExtractableResponse<Response> response, LineRequest params) {
        String uri = response.header("Location");

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().put(uri)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 지하철_노선_수정_요청(Long lineId, LineRequest params) {
        String uri = "/lines/" + lineId;

        return RestAssured
                .given().log().all()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(params)
                .when().put(uri)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 지하철_노선_제거_요청(ExtractableResponse<Response> response) {
        String uri = response.header("Location");

        return RestAssured
                .given().log().all()
                .when().delete(uri)
                .then().log().all()
                .extract();
    }

    public static ExtractableResponse<Response> 지하철_노선_제거_요청(Long lineId) {
        String uri = "/lines/" + lineId;

        return RestAssured
                .given().log().all()
                .when().delete(uri)
                .then().log().all()
                .extract();
    }

    public static void 지하철_노선_생성됨(ExtractableResponse response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
        assertThat(response.header("Location")).isNotBlank();
    }

    public static ExtractableResponse<Response> 지하철_노선_생성되어_있음(LineRequest lineRequest) {
        ExtractableResponse<Response> createResponse = 지하철_노선_생성_요청(lineRequest);
        지하철_노선_생성됨(createResponse);

        return createResponse;
    }

    public static void 지하철_노선_생성_실패됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    public static void 지하철_노선_수정_실패됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    public static void 지하철_노선_제거_실패됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    public static void 지하철_노선_목록_응답됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 지하철_노선_응답됨(ExtractableResponse<Response> response, ExtractableResponse<Response> createdResponse) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.as(LineResponse.class)).isNotNull();
    }

    public static void 지하철_노선_목록_포함됨(ExtractableResponse<Response> response, List<ExtractableResponse<Response>> createdResponses) {
        List<Long> expectedLineIds = createdResponses.stream()
                .map(it -> Long.parseLong(it.header("Location").split("/")[2]))
                .collect(Collectors.toList());

        List<Long> resultLineIds = response.jsonPath().getList(".", LineResponse.class).stream()
                .map(LineResponse::getId)
                .collect(Collectors.toList());

        assertThat(resultLineIds).containsAll(expectedLineIds);
    }

    public static void 지하철_노선_수정됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    public static void 지하철_노선_삭제됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    public static void 두_노선에_겹치는_역이_존재함(
            ExtractableResponse<Response> lineResponse1, ExtractableResponse<Response> lineResponse2
    ) {
        LineResponse line1 = lineResponse1.as(LineResponse.class);
        LineResponse line2 = lineResponse2.as(LineResponse.class);

        List<Long> line1StationIds = line1.getStations().stream()
                .map(StationResponse::getId)
                .collect(Collectors.toList());

        List<Long> line2StationIds = line2.getStations().stream()
                .map(StationResponse::getId)
                .collect(Collectors.toList());

        assertThat(line1StationIds).containsAnyElementsOf(line2StationIds);
    }
}