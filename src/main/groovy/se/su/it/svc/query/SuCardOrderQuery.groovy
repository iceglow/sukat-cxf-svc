package se.su.it.svc.query

import groovy.sql.GroovyRowResult
import groovy.sql.Sql
import groovy.util.logging.Slf4j
import org.apache.commons.dbcp.BasicDataSource
import se.su.it.svc.commons.SvcCardOrderVO

import java.sql.Timestamp

@Slf4j
class SuCardOrderQuery {

  def suCardDataSource

  private final int DEFAULT_ORDER_STATUS = 3 // WEB (online order)

  public List findAllCardOrdersForUid(String uid) {

    log.info "Querying card orders for uid: $uid"

    def query = "SELECT r.id,serial,owner,printer,createTime,firstname,lastname,streetaddress1,streetaddress2,locality,zipcode,value,description FROM request r JOIN address a ON r.address = a.id JOIN status s ON r.status = s.id WHERE r.owner = :uid"
    def args = [uid:uid]

    Closure queryClosure = { Sql sql ->
      if (!sql) { return null }
      return sql?.rows(query, args)
    }

    def rows = withConnection(queryClosure)

    if (!rows) { return [] }

    log.info "Found ${rows?.size()} order entries in the database."

    def cardOrders = []

    for (row in rows) {
      try {
        SvcCardOrderVO svcCardOrderVO = new SvcCardOrderVO( row as GroovyRowResult )
        log.debug "Adding card order ${svcCardOrderVO?.id} to $uid's orders."
        cardOrders << svcCardOrderVO
      } catch (ex) {
        log.error "Failed to add order $row to orders.", ex
      }

    }

    return cardOrders
  }

  public String orderCard(SvcCardOrderVO cardOrderVO) {

    String uuid = ''

    try {
     uuid = findFreeUUID()
    } catch (ex) {
      log.error "Failed when fetching free uuid", ex
    }

    if (!uuid) {
      return uuid
    }

    try {
      def addressQuery = "INSERT INTO address VALUES(null, :streetaddress1, :streetaddress2, :locality, :zipcode)"
      def addressArgs = [
          streetaddress1:cardOrderVO.streetaddress1,
          streetaddress2:cardOrderVO.streetaddress2,
          locality:cardOrderVO.locality,
          zipcode:cardOrderVO.zipcode
      ]

      def requestQuery = "INSERT INTO request VALUES(:id, :serial, :owner, :printer, :createTime, :address, :status, :firstname, :lastname)"
      def requestArgs = [
        id: uuid,
        serial: cardOrderVO.serial,
        owner: cardOrderVO.owner,
        printer: cardOrderVO.printer,
        createTime: new Timestamp(new Date().getTime()),
        firstname: cardOrderVO.firstname,
        lastname: cardOrderVO.lastname,
        address: null,
        status: DEFAULT_ORDER_STATUS
      ]

      Closure queryClosure = { Sql sql ->
        if (!sql) { return null }

        sql.withTransaction {
          sql.withBatch {
            def addressResponse = sql?.executeInsert(addressQuery, addressArgs)
            requestArgs['address'] = addressResponse[0][0] // Get the address id and set it as the request address id.
            def requestResponse = sql?.executeInsert(requestQuery, requestArgs)
            return requestResponse[0][0] // return the id of the insert. (should be the same as the supplied uuid.)
          }
        }

      }

      uuid = withConnection(queryClosure)

      log.info "Card order successfully added to database!"
    } catch (ex) {
      log.error "Failed to create card order for ${cardOrderVO.owner}", ex
    }

    log.info "Returning $uuid"

    return uuid
  }

  private String findFreeUUID() {
    /** WHYYY use uuids :~/ */
    boolean newUUID = false
    String uuid = ''
    String query = "SELECT id FROM request WHERE id = :uuid"

    while (!newUUID) {
      uuid = UUID.randomUUID().toString()
      log.info "findFreeUUID: Querying for uuid: ${uuid}"
      def result = withConnection({Sql sql ->
        return sql.rows(query, [uuid:uuid])
      })

      if (result?.size() == 0) {
        newUUID = true
      } else {
        log.info "findFreeUUID: ${uuid} was already take, retrying."
      }
    }
    log.info "findFreeUUID: Returning free uuid ${uuid}"
    return uuid
  }

  private withConnection = { Closure query ->
    def response = null
    Sql sql = null
    try {
      /** getDataSource added for mock and testing purposes */
      sql = new Sql(suCardDataSource)
      response = query(sql)
    } catch (ex) {
      log.error "Connection to SuCardDB failed", ex
    } finally {
      try {
        sql.close()
      } catch (ex) {
        log.error "Failed to close connection", ex
      }
    }
    return response
  }

}