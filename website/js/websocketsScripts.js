/**
 * Created by cba62 on 21/09/17.
 */

var mySocket = null;
createGameRecorderSocket();

/**
 * Creates the header for the AC35 protocol messages
 * @param type The type of the message
 * @param messageLength The length of the payload
 */
createHeader = function (type, messageLength) {
    let HEADER_LENGTH = 15;
    let header = (function (s) {
        let a = [];
        while (s-- > 0)
            a.push(0);
        return a;
    })(HEADER_LENGTH);
    header[0] = (71 | 0);
    header[1] = (131 | 0);
    this.addIntIntoByteArray(header, 2, 1, type);
    this.addIntIntoByteArray(header, 13, 2, messageLength);
    return header;
}

/**
 * Puts an int into its bytes and put them into a byte array
 * @param array The array to be put into
 * @param start The starting index to put number into
 * @param numBytes The number of bytes of the int
 * @param item The actual value of the int
 */
addIntIntoByteArray = function (array, start, numBytes, item) {
    for (let i = 0; i < numBytes; i++) {
        array[start + i] = (item >> (i * 8)) & (0xFF);
    }
};

/**
 * Creates a WebSocket connection to the Game Recorder Server
 */
function createGameRecorderSocket() {
    mySocket = new WebSocket("ws://127.0.0.1:2827"); // 2827 is the port game server runs on
    mySocket.binaryType = 'arraybuffer';

    mySocket.onerror = function (event) {
        console.log(event);
    }

    mySocket.onopen = function () {
        console.log("WebSocket connection established.");
    };

    mySocket.onclose = function () {
        alert("No connection to GameRecorder");
    }
}

/**
 * Sends a request game packet to the Game Recorder
 * @param code The room code entered
 */
requestGame = function(code) {
    if(mySocket == null){
        createGameRecorderSocket();
    }
    var header = createHeader(114, 2);
    let body = [0, 0];
    addIntIntoByteArray(body, 0, 2, code);
    var crc = createCrc(header, body);
    var packet = header.concat(body).concat(crc);
    console.log(packet);
    var byteArray = new Uint8Array(packet);
    console.log(byteArray.buffer);
    mySocket.send(byteArray.buffer);
}

/**
 * Calculates and return the CRC over the header and body
 * @param header The header of the packet
 * @param body The body of the packet
 * @returns {[number,number,number,number]} The CRC of the packet
 */
createCrc = function(header, body){
    let both = header.concat(body);
    let crc = parseInt(crc32(both), 16);
    let array = [0, 0, 0, 0];
    addIntIntoByteArray(array, 0, 4, crc);
    return array;
}
