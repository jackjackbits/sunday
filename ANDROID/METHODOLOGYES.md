Metodología

El objetivo es determinar la cantidad de tiempo que una persona necesita pasar bajo el sol para producir una cantidad saludable de vitamina D, sin quemarse.

1. Tiempo para Quemarse

Para determinar el tiempo que tarda la piel en quemarse, utilizamos el concepto de Dosis Mínima Eritematosa (MED, por sus siglas en inglés). Una MED es la cantidad de exposición a la radiación UV que causa un enrojecimiento perceptible de la piel (eritema). Se considera que 1 MED equivale a aproximadamente 2 SED (Dosis de Eritema Estándar).

Usamos la siguiente fórmula para calcular el tiempo para alcanzar 1 MED:

minutos_para_quemarse = (MED_personal * 60) / (UVI * 1.5)
Donde:

MED_personal se basa en el tipo de piel de Fitzpatrick del individuo.

UVI es el Índice UV actual.

1.5 es un factor de ajuste.

2. Tiempo para Obtener Vitamina D

El tiempo necesario para sintetizar vitamina D es generalmente menor que el tiempo para quemarse. Para la mayoría de las personas de piel clara, se estima que la producción de 1000 a 2000 UI (Unidades Internacionales) de vitamina D requiere aproximadamente 0.5 MED.

Utilizamos la siguiente fórmula para calcular el tiempo para obtener una dosis saludable de vitamina D:

minutos_para_vitamina_d = (MED_personal * 0.5 * 60) / (UVI * 1.5)
Esta fórmula es esencialmente la mitad del tiempo que se tarda en quemarse.

Fuentes de Datos

Índice UV (UVI): Obtenido de la API de OpenUV, que proporciona datos del índice UV en tiempo real basados en la ubicación del usuario.

Tipos de Piel de Fitzpatrick: Utilizamos la escala de Fitzpatrick para clasificar los tipos de piel y determinar la MED personal de un individuo.

Valores de MED Personal por Tipo de Piel

Tipo de Piel	Descripción	MED (J/m²)
I	Siempre se quema, nunca se broncea	200
II	Se quema fácilmente, se broncea mínimamente	250
III	Se quema moderadamente, se broncea gradualmente	350
IV	Se quema mínimamente, siempre se broncea bien	450
V	Rara vez se quema, se broncea abundantemente	600
VI	Nunca se quema, pigmentación oscura	1000

Exportar a Hojas de cálculo
Nota: Estos valores de MED son aproximaciones y pueden variar entre individuos.

Explicación de la Metodología
Ahora te explico qué significa todo esto en términos más sencillos.

El objetivo de la app es muy simple: darte una "ventana de tiempo segura" para tomar el sol. Quieres el beneficio (vitamina D) sin el riesgo (quemadura). Para ello, la app realiza dos cálculos principales:

Paso 1: Calcular cuánto tardas en quemarte
El Concepto Clave: "Dosis Mínima Eritematosa" (MED). Imagina que es tu "límite personal de sol". Es la cantidad exacta de energía UV que tu piel puede recibir antes de empezar a ponerse roja.

¿Cómo sabe tu límite? Usa la Escala de Fitzpatrick, que es un estándar científico para clasificar tipos de piel. Si tienes la piel muy clara (Tipo I), tu límite (MED) es bajo. Si tu piel es muy oscura (Tipo VI), tu límite es mucho más alto. Tú le dices a la app qué tipo de piel tienes.

El Factor Externo: El Índice UV (UVI). Este número te dice qué tan fuerte está el sol en tu ubicación y en ese preciso momento. Un UVI de 10 es mucho más intenso que un UVI de 2. La app obtiene este dato en tiempo real usando tu GPS.

La Fórmula Simple:
Tiempo para Quemarse = (Tu Límite Personal) / (Intensidad del Sol Actual)

La app toma tu MED (límite personal), lo divide por la intensidad del sol (UVI) y te da el resultado en minutos. Así sabes cuál es tu tiempo máximo de exposición antes de sufrir una quemadura.

Paso 2: Calcular cuánto tiempo necesitas para generar Vitamina D
La Buena Noticia: Producir vitamina D es mucho más rápido que quemarse. La ciencia ha demostrado que para la mayoría de la gente, solo se necesita la mitad de la dosis de UV que te causaría una quemadura para generar una buena cantidad de vitamina D (entre 1000 y 2000 UI).

La Fórmula Simple:
Tiempo para Vitamina D = (Tiempo para Quemarse) / 2

La app simplemente toma el tiempo máximo que calculó en el paso anterior y lo divide por la mitad.

El Resultado Final para Ti
La app te presenta estos dos números para que puedas tomar una decisión informada.

Ejemplo Práctico:

Tus datos: Tienes piel Tipo III (MED de 350) y en tu ciudad el Índice UV ahora mismo es de 7.

Cálculo de la app:

Tiempo para Quemarse: (350 * 60) / (7 * 1.5) ≈ 33 minutos. Este es tu límite máximo.

Tiempo para Vitamina D: 33 minutos / 2 ≈ 16.5 minutos.

Lo que te muestra la app:

"Para obtener una dosis saludable de Vitamina D, exponte al sol durante unos 16 minutos. Evita pasar más de 33 minutos para no quemarte."
## References

- Holick, M.F. (2007). "Vitamin D deficiency." New England Journal of Medicine
- Webb, A.R. et al. (2018). "The role of sunlight exposure in determining the vitamin D status"
- Engelsen, O. (2010). "The relationship between ultraviolet radiation exposure and vitamin D status"
- MacLaughlin, J. & Holick, M.F. (1985). "Aging decreases the capacity of human skin to produce vitamin D3"
- Çekmez, Y. et al. (2024). "Time and duration of vitamin D synthesis" PMC10861575
- Various studies on MED and safe sun exposure from SunSmart Australia
