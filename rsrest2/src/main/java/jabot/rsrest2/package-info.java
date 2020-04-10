/**
 * REST V2 (http://127.0.0.1:9090/api/v2) retroshare connector.
 * 
 * Frontend class is {@link RetroshareV2}, others are not to be used by client.
 * 
 * Implemented as jersey client because:
 * <ul>
 *   <li>Jersey has builtin Jackson that is very easy to use on POJOs</li>
 *   <li>Jersey has all needed HTTP support</li>
 * </ul>
 * 
 */
package jabot.rsrest2;
