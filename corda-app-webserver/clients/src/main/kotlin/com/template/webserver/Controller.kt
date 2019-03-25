package com.template.webserver

import org.jgroups.protocols.DISCARD_PAYLOAD
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startTrackedFlow
import com.template.IOUFlow
import com.template.IOUState
import net.corda.core.contracts.StateAndRef
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.springframework.http.HttpStatus
/**
 * Define your API endpoints here.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/templateendpoint", produces = arrayOf("text/plain"))
    private fun templateendpoint(): String {
        return "Define an endpoint here."
    }

    @GetMapping(value = "/getTransact", produces = arrayOf("text/plain"))
    private fun getTransaction(): String {
        val result = proxy.vaultQueryBy<IOUState>().states.first().state.data
        return result.toString()
    }

//    @GetMapping(value = "/getTransact")
//    fun getMyIOUs(): ResponseEntity<List<StateAndRef<IOUState>>>  {
//        val myious = proxy.vaultQueryBy<IOUState>().states
//        return ResponseEntity.ok(myious)
//    }

    @PostMapping(value = "/Transact")
    private  fun TransactionOne(@RequestParam(value = "payload") payload: String, @RequestParam(value = "partyName") partyName: String): ResponseEntity<String> {
        val partyX500Name = CordaX500Name.parse(partyName)
        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $partyName cannot be found.\n")
        return try {
            val signedTx = proxy.startTrackedFlow(::IOUFlow, payload, otherParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


}