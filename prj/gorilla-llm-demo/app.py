import openai
import streamlit as st
import subprocess

openai.api_key = "EMPTY"
openai.api_base = "http://zanino.millemmium.edu:8000/v1"

# Gorilla Server
def get_gorilla_response(prompt, model):
    try:
        completion = openai.ChatCompletion.create(
            model = model,
            messages = [{"role": "user", "content":prompt}]
        )
        print("Response: ", completion)
        return completion.choices[0].message.content
    except Exception as e:
        print("Sorry, something went wrong!") 

def extract_code_from_output(output):
    code = output.split("code>>>:")[1]
    return code           

def run_geneated_code(file_path):
    command = ["python", file_path]

    try:
        # Execute the command
        result = subprocess.run(command, stdout = subprocess.PIPE, stderr = subprocess.PIPE, text = True)

        # Check if the subprcess ran successfully
        if result.returncode == 0:
            st.success("Generated code executed successfully.")
            # Display the output
            st.code(result.stdout, language = "python")
        else:
            st.error("Generated code execution failed with the following error:")
            # Display the error message
            st.code(result.stderr, language="bash")
    except Exception as e:
            st.error("Error occurred while running the generated code:", e)
        
st.set_page_config(layout="wide")

def main():
    st.title("Gorilla LLM Demo App 🦍‍👤")
    
    input_prompt = st.text_area("Enter yoru prompt below:")

    option = st.selectbox("Select a model option from the list:", ('gorilla-7b-hf-v1', 'gorilla-mpt-7b-hf-v0'))

    if st.button("Gorilla Magic"):
        if len(input_prompt) > 0:
            col1, col2 = st.columns([1,1])
            with col1:
                if option == "gorilla-7b-hf-v1":
                    result = get_gorilla_response(prompt=input_prompt, model=option)
                    st.write(result)
                elif option == "gorilla-mpt-7b-hf-v0":
                    result = get_gorilla_response(prompt=input_prompt, model=option)
                    st.write(result)

            with col2:
                if option == 'gorilla-7b-hf-v1':
                    code_result = extract_code_from_output(result)
                    st.subheader("Generated Output")
                    st.code(code_result, language='python')
                    file_path = "generated_code_gorilla_7b_hf_v1.py"
                    with open(file_path, 'w') as file:
                        file.write(code_result)
                elif option == "gorilla-mpt-7b-hf-v0":
                    code_result = extract_code_from_output(result)
                    lines = code_result.split("\\n")
                    for i in range(len(lines)-1):
                        st.code(lines[i], language='python')
                    file_path = "generated_code_gorilla_mpt_7b_hf_v0.py"
                    with open(file_path, 'w') as f:
                        f.write(lines[i].strip().replace('\\"', '"') + '\n')
                run_geneated_code(file_path)

if __name__ == "__main__":
    main()