package uk.nhs.nhsx.covid19.android.app.remote

import uk.nhs.nhsx.covid19.android.app.common.TranslatableString
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.questionnaire.selection.Symptom
import uk.nhs.nhsx.covid19.android.app.remote.data.QuestionnaireResponse

class MockQuestionnaireApi : QuestionnaireApi {
    private val successResponse = QuestionnaireResponse(
        symptoms = listOf(
            Symptom(
                title = TranslatableString(
                    mapOf(
                        "en-GB" to "A high temperature (fever)",
                        "bn-BD" to "উচ্চ তাপমাত্রা (জ্বর)",
                        "gu-IN" to "ઉંચુ તાપમાન (તાવ)",
                        "ur-PK" to "زیادہ درجہ حرارت (بخار)",
                        "pa-IN" to "ਉੱਚ ਤਾਪਮਾਨ (ਬੁਖ਼ਾਰ)",
                        "cy" to "A high temperature (fever)"
                    )
                ),
                description = TranslatableString(
                    mapOf(
                        "en-GB" to "This means that you feel hot to touch on your chest or back (you do not need to measure your temperature).",
                        "bn-BD" to "এর অর্থ আপনার বুক বা পিঠে স্পর্শ করলে উষ্ণ বোধ হয় (আপনাকে আপনার তাপমাত্রা পরিমাপ করার প্রয়োজন নেই)।",
                        "gu-IN" to "આનો મતલબ કે તમને છાતી કે પીઠ પર સ્પર્શ કરતા ગરમ અનુભવાય (તમારે તાપમાન માપવાની જરુર નથી).",
                        "ur-PK" to "اس کا مطلب یہ ہے کہ آپ کو اپنے سینے یا پیٹھ کو چھونے پر گرم محسوس ہوتا ہے (آپ کو اپنے درجہ حرارت کی پیمائش کرنے کی ضرورت نہیں ہے)۔",
                        "pa-IN" to "ਇਸਦਾ ਮਤਲਬ ਹੁੰਦਾ ਹੈ ਕਿ ਆਪਣੀ ਛਾਤੀ ਜਾਂ ਪਿੱਠ ਨੂੰ ਛੂਹਣ 'ਤੇ ਤੁਸੀਂ ਗਰਮ ਮਹਿਸੂਸ ਹੁੰਦੇ ਹੋ (ਤੁਹਾਨੂੰ ਆਪਣੇ ਤਾਪਮਾਨ ਨੂੰ ਮਾਪਣ ਦੀ ਜ਼ਰੂਰਤ ਨਹੀਂ ਹੈ)।",
                        "cy" to "This means that you feel hot to touch on your chest or back (you do not need to measure your temperature)."
                    )
                ),
                riskWeight = 1.0
            ),
            Symptom(
                title = TranslatableString(
                    mapOf(
                        "en-GB" to "A new continuous cough",
                        "bn-BD" to "নতুন একটানা কাশি",
                        "gu-IN" to "નવો સતત કફ",
                        "ur-PK" to "نئی مسلسل کھانسی",
                        "pa-IN" to "ਨਵੀਂ ਲੱਗੀ ਲਗਾਤਾਰ ਆਉਣ ਵਾਲੀ ਖਾਂਸੀ",
                        "cy" to "A new continuous cough"
                    )
                ),
                description = TranslatableString(
                    mapOf(
                        "en-GB" to "This means coughing a lot for more than an hour, or 3 or more coughing episodes in 24 hours (if you usually have a cough, it may be worse than usual).",
                        "bn-BD" to " এর অর্থ এক ঘণ্টারও বেশি সময় ধরে খুব কাশি, বা 24 ঘন্টার মধ্যে 3 বা ততোধিক কাশির পর্ব (আপনার যদি সাধারণ কাশি থেকে থাকে তবে এটি হয়তো স্বাভাবিকের চেয়েও খারাপ হতে পারে)। ",
                        "gu-IN" to "આનો મતલબ કે એક કલાક કરતા વધુ સમય માટે કફ ચાલુ કહે કે 24 કલાકમાં 3 વાર કફ આવે (સામાન્યરીતે તમને કફ આવતો હોય તો સ્થિતિ સામાન્ય કરતા ખરાબ હોઈ શકે છે).",
                        "ur-PK" to "اس کا مطلب یہ ہے کہ آپ ایک گھنٹے سے زیادہ دیر تک بہت زیادہ کھانستے ہیں یا 24 گھنٹے میں کھانسنے کا سلسلہ 3 یا اس سے زائد بار پیش آتا ہے (اگر آپ کو عموماً کھانسی رہتی ہے تو یہ معمول سے زیادہ بدتر ہو سکتی ہے)۔",
                        "pa-IN" to "ਇਸਦਾ ਮਤਲਬ ਹੈ ਇੱਕ ਘੰਟੇ ਤੋਂ ਵੱਧ ਸਮੇਂ ਲਈ ਖੰਘ ਆਉਣਾ, ਜਾਂ 24 ਘੰਟਿਆਂ ਵਿੱਚ 3 ਜਾਂ ਵਧੇਰੇ ਖੰਘ ਦੇ ਦੌਰ ਆਉਣਾ (ਜੇ ਤੁਹਾਨੂੰ ਆਮ ਤੌਰ 'ਤੇ ਖੰਘ ਹੁੰਦੀ ਹੈ, ਤਾਂ ਇਹ ਆਮ ਨਾਲੋਂ ਬਦਤਰ ਹੋ ਸਕਦੀ ਹੈ)।",
                        "cy" to "This means coughing a lot for more than an hour, or 3 or more coughing episodes in 24 hours (if you usually have a cough, it may be worse than usual)."
                    )
                ),
                riskWeight = 1.0
            ),
            Symptom(
                title = TranslatableString(
                    mapOf(
                        "en-GB" to "A new loss or change to your sense of smell or taste",
                        "bn-BD" to "আপনার গন্ধ বা স্বাদের অনুভূতির পরিবর্তন বা অনুভূতি হারানো",
                        "gu-IN" to "તમારી ધ્રાણેન્દ્રિય કે સ્વાદમાં નવી ક્ષતિ કે બદલાવ",
                        "ur-PK" to "آپ کے بو یا ذائقہ کی حس کا نئے طریقے سے ختم یا تبدیل ہونا",
                        "pa-IN" to "ਗੰਧ ਜਾਂ ਸਵਾਦ ਵਿੱਚ ਨਵੀਂ-ਨਵੀਂ ਆਈ ਕਮੀ ਜਾਂ ਬਦਲਾਅ",
                        "cy" to "A new loss or change to your sense of smell or taste"
                    )
                ),
                description = TranslatableString(
                    mapOf(
                        "en-GB" to "This means you have noticed you cannot smell or taste anything, or things smell or taste different to normal.",
                        "bn-BD" to "এর অর্থ আপনি লক্ষ্য করেছেন যে আপনি কোনও কিছুর গন্ধ বা স্বাদ পাচ্ছেন না বা জিনিসের গন্ধ বা স্বাদ স্বাভাবিকের থেকে আলাদা।",
                        "gu-IN" to "આનો મતલબ કે તમારા ધ્યાન પર આવ્યુ છે કે તમારુ નાક કશુ સુંધી શકતું નથી કે કશાનો સ્વાદ આવતો નથી કે સામાન્ય સ્થિતિ કરતા અલગ પ્રકારે સ્વાદ અને સુગંધ આવે છે.",
                        "ur-PK" to "اس کا مطلب ہے کہ آپ کو یہ محسوس ہوا ہے کہ آپ کوئی چیز سونگھ نہیں سکتے یا کسی چیز کا ذائقہ معلوم نہیں ہو رہا ہے یا پھر چیزوں کی بو یا ذائقہ معمول سے الگ معلوم ہو رہا ہے۔",
                        "pa-IN" to "ਇਸ ਦਾ ਅਰਥ ਹੈ ਕਿ ਤੁਸੀਂ ਨੋਟ ਕੀਤਾ ਹੈ ਕਿ ਤੁਸੀਂ ਕਿਸੇ ਚੀਜ਼ ਦੀ ਗੰਧ ਜਾਂ ਸਵਾਦ ਨਹੀਂ ਲੈ ਸਕਦੇ, ਜਾਂ ਸੁੰਘਣ 'ਤੇ ਚੀਜ਼ਾਂ ਵਿੱਚੋਂ ਸਧਾਰਨ ਨਾਲੋਂ ਵੱਖਰੀ ਗੰਧ ਆਉਂਦੀ ਹੈ।",
                        "cy" to "This means you have noticed you cannot smell or taste anything, or things smell or taste different to normal."
                    )
                ),
                riskWeight = 1.0
            ),
            Symptom(
                title = TranslatableString(
                    mapOf(
                        "en-GB" to "Dummy",
                        "bn-BD" to "Dummy",
                        "gu-IN" to "Dummy",
                        "ur-PK" to "Dummy",
                        "pa-IN" to "Dummy",
                        "cy" to "Dummy"
                    )
                ),
                description = TranslatableString(
                    mapOf(
                        "en-GB" to "Dummy and not related with coronavirus",
                        "bn-BD" to "Dummy and not related with coronavirus",
                        "gu-IN" to "Dummy and not related with coronavirus",
                        "ur-PK" to "Dummy and not related with coronavirus",
                        "pa-IN" to "Dummy and not related with coronavirus",
                        "cy" to "Dummy and not related with coronavirus"
                    )
                ),
                riskWeight = 0.0
            )
        ),
        riskThreshold = 0.5f,
        symptomsOnsetWindowDays = 5
    )

    override suspend fun fetchQuestionnaire(): QuestionnaireResponse =
        MockApiModule.behaviour.invoke { successResponse }
}
