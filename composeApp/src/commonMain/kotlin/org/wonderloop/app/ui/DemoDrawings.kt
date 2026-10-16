package org.wonderloop.app.ui

/**
 * 4 Pre-packaged demo drawings for quick testing and evaluation on Fire TV Stick 4K
 * or Android TV emulator without needing a secondary mobile device.
 */
data class DemoDrawingItem(
    val id: String,
    val name: String,
    val emoji: String,
    val species: String,
    val description: String,
    val base64DataUri: String
)

object DemoDrawings {
    val items = listOf(
        DemoDrawingItem(
            id = "demo_dragon",
            name = "Dragón Chispa",
            emoji = "🐲",
            species = "dragón verde",
            description = "Un dragón verde simpático con alas amarillas y escamas brillantes",
            // Clean 100x100 green dragon sketch placeholder JPEG
            base64DataUri = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAAMAAwBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
        ),
        DemoDrawingItem(
            id = "demo_dog",
            name = "Perrito Toby",
            emoji = "🐶",
            species = "perrito marrón",
            description = "Un cachorro marrón con orejas caídas y pañuelo azul al cuello",
            base64DataUri = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAAMAAwBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
        ),
        DemoDrawingItem(
            id = "demo_robot",
            name = "Robot Bip",
            emoji = "🤖",
            species = "robot plateado",
            description = "Un robot plateado con antena de estrella y corazón rojo que parpadea",
            base64DataUri = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAAMAAwBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
        ),
        DemoDrawingItem(
            id = "demo_unicorn",
            name = "Unicornio Luz",
            emoji = "🦄",
            species = "unicornio blanco",
            description = "Un unicornio blanco con cuerno dorado y crin de arcoíris",
            base64DataUri = "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////wgALCAAMAAwBAREA/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/aAAgBAQABPxA="
        )
    )
}
