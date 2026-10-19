# -*- coding: utf-8 -*-
"""
=============================================================================
KỊCH BẢN HUẤN LUYỆN MÔ HÌNH PHÂN LOẠI Ý ĐỊNH (INTENT CLASSIFIER - NLP)
Dự án: Hệ thống Quản lý Sân Cầu Lông UTE Sport
Thuật toán: TF-IDF (N-gram 1-2) + Logistic Regression / Naive Bayes
=============================================================================
"""

import json
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')
if hasattr(sys.stderr, 'reconfigure'):
    sys.stderr.reconfigure(encoding='utf-8')

def remove_accents(input_str):
    """Chuẩn hóa loại bỏ dấu tiếng Việt để tăng khả năng bao phủ"""
    import unicodedata
    s1 = u'ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝàáâãèéêìíòóôõùúýĂăĐđĨĩŨũƠơƯưẠạẢảẤấẦầẨẩẪẫẬậẮắẰằẲẳẴẵẶặẸẹẺẻẼẽẾếỀềỂểỄễỆệỈỉỊịỌọỎỏỐốỒồỔổỖỗỘộỚớỜờỞởỠỡỢợỤụỦủỨứỪừỬửỮữỰựỲỳỴỵỶỷỸỹ'
    s0 = u'AAAAEEEIIOOOOUUYaaaaeeeiioooouuyAaDdIiUuOoUuAaAaAaAaAaAaAaAaAaAaAaAaEeEeEeEeEeEeEeEeIiIiOoOoOoOoOoOoOoOoOoOoOoOoUuUuUuUuUuUuUuYyYyYyYy'
    s = ''
    input_str = unicodedata.normalize('NFD', input_str)
    for c in input_str:
        if unicodedata.category(c) != 'Mn':
            s += c
    return s.lower()

def main():
    dataset_path = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "nlp", "intent_dataset.json")
    
    if not os.path.exists(dataset_path):
        print(f"Lỗi: Không tìm thấy file dữ liệu tại {dataset_path}")
        return

    with open(dataset_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    texts = []
    labels = []
    intent_responses = {}

    for item in data.get("intents", []):
        tag = item["tag"]
        intent_responses[tag] = item.get("responses", [""])[0]
        for pattern in item.get("patterns", []):
            texts.append(pattern)
            labels.append(tag)
            # Thêm cả dạng không dấu vào dataset để nhân đôi độ phủ
            no_acc = remove_accents(pattern)
            if no_acc != pattern.lower():
                texts.append(no_acc)
                labels.append(tag)

    print("=================================================================")
    print(f" TỔNG SỐ MẪU HUẤN LUYỆN (TRAINING SAMPLES): {len(texts)}")
    print(f" CÁC NHÃN Ý ĐỊNH (INTENTS): {list(intent_responses.keys())}")
    print("=================================================================")

    try:
        from sklearn.feature_extraction.text import TfidfVectorizer
        from sklearn.linear_model import LogisticRegression
        from sklearn.pipeline import make_pipeline
        from sklearn.model_selection import cross_val_score
    except ImportError:
        print("\n[CHÚ Ý] scikit-learn chưa được cài đặt trong môi trường Python.")
        print("Bạn có thể cài đặt bằng lệnh: pip install scikit-learn numpy\n")
        print("Tuy nhiên, Java Spring Boot đã có sẵn bộ mã nguồn tự tính TF-IDF và Cosine độc lập 100%!")
        return

    # Xây dựng Pipeline: TF-IDF (unigram + bigram) + Logistic Regression
    pipeline = make_pipeline(
        TfidfVectorizer(ngram_range=(1, 2), min_df=1, sublinear_tf=True),
        LogisticRegression(max_iter=1000, C=5.0)
    )

    # Huấn luyện mô hình
    pipeline.fit(texts, labels)
    
    # Đánh giá Cross Validation
    scores = cross_val_score(pipeline, texts, labels, cv=3)
    print(f"ĐỘ CHÍNH XÁC TRUNG BÌNH (Cross-Validation Accuracy): {scores.mean() * 100:.2f}%\n")

    # Thử nghiệm các câu hỏi thực tế (Kể cả sai chính tả, câu ngoài lề)
    test_queries = [
        "tối nay 18h còn sân trống ko shop",
        "thuê sân 2 tiếng hết bao nhiêu tiền",
        "chốt đặt sân cho mình nha",
        "hủy lịch chơi có được lấy lại tiền cọc ko",
        "địa chỉ chi nhánh thủ đức ở đâu vậy",
        "giải phương trình bậc hai 2x + 10 = 20",
        "hôm nay trời có mưa không bạn ơi",
        "alo xin chào bot",
        "kiểm tra mã đơn DS001234 giùm mình"
    ]

    print("--- KIỂM THỬ THỰC TẾ (INFERENCE TEST) ---")
    THRESHOLD = 0.45  # Ngưỡng tin cậy

    for query in test_queries:
        probs = pipeline.predict_proba([query])[0]
        max_idx = probs.argmax()
        predicted_tag = pipeline.classes_[max_idx]
        confidence = probs[max_idx]

        if confidence < THRESHOLD:
            final_tag = "UNKNOWN_OUT_OF_SCOPE"
            reply = "Dạ tôi là bot sân cầu lông, chưa hiểu câu hỏi này của bạn ạ!"
        else:
            final_tag = predicted_tag
            reply = intent_responses.get(predicted_tag, "Dạ tôi đã hiểu yêu cầu.")

        print(f"Câu hỏi: \"{query}\"")
        print(f" -> Nhãn dự đoán: {final_tag} (Độ tin cậy: {confidence * 100:.1f}%)")
        print(f" -> Phản hồi: {reply[:80]}...\n")

if __name__ == "__main__":
    main()
