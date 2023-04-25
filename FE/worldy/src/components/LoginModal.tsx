import * as React from 'react';
import { useState, useEffect } from 'react';
import GameController from '../assets/images/GameController.png';
import LoginLogo from '../assets/images/LoginLogo.png';
import KakaoLoginButton from '../assets/images/KakaoLoginButton.png';
import LoaderPyramid from './LoaderPyramid';
import KakaoLogin from './KakaoLogin';
import axios from 'axios';
import { access } from 'fs';

type PointerOutProps = {
  onClose: () => void;
  onClickKakaoLogin: (firstLogin: boolean) => void;
};
export default function LoginModal({
  onClose,
  onClickKakaoLogin,
}: PointerOutProps) {
  const [loadedLoginLogo, setLoadedLoginLogo] = useState<boolean>(false);
  const [loadedGameController, setLoadedGameController] =
    useState<boolean>(false);

  const LoginLogoImg = new Image();
  const GameControllerImg = new Image();
  useEffect(() => {
    LoginLogoImg.src = LoginLogo;
    GameControllerImg.src = GameController;
    LoginLogoImg.onload = () => {
      setTimeout(() => {
        setLoadedLoginLogo(true);
      }, 300);
      console.log('LoginLogo 로드');
    };
    GameControllerImg.onload = () => {
      setTimeout(() => {
        setLoadedGameController(true);
      }, 300);
      console.log('GameController 로드');
    };
  }, [LoginLogo, GameController]);

  const firstLogin: boolean = true;

  const clickKakaoLogin = () => {
    //게임 시작 눌렀을 때 처음 로그인이면 튜토리얼로, 아니면 메인페이지로
    onClickKakaoLogin(firstLogin);
  };

  ////////////////////카카오 로그인 버튼 관련

  const handleKakaoLoginSuccess = async (response: any) => {
    console.log('카카오 로그인 성공', response);
    const accessToken = response.access_token;

    try {
      const userInfoResponse = await axios.get(
        'https://kapi.kakao.com/v2/user/me',
        {
          headers: {
            Authorization: `Bearer ${accessToken}`,
          },
        }
      );

      const userData = userInfoResponse.data;
      console.log('유저 정보', userData);
      onClickKakaoLogin(firstLogin);
    } catch (error) {
      console.error('유저 정보 가져오기 실패', error);
    }
  };

  const [showErrorModal, setShowErrorModal] = useState<boolean>(false);

  const handleKakaoLoginFailure = (error: any) => {
    console.log('카카오 실패', error);
    setShowErrorModal(true);
  };

  const closeErrorModal = () => {
    setShowErrorModal(false);
  };

  return (
    <div
      // onClick={onClose}
      className='fixed top-4 left-0 w-full h-full bg-behindModalBackground z-10 flex flex-row justify-center items-center'
    >
      <div
        id='LoginWholeModalFrame'
        className='bg-white py-5 px-10 rounded-xl w-1/3 h-3/4 flex flex-col z-20'
        onClick={undefined}
      >
        {loadedLoginLogo && loadedGameController ? (
          <div>
            <div id='XbuttonFrame' className='flex flex-row justify-end'>
              <button onClick={onClose}>
                <div>
                  <svg
                    stroke='currentColor'
                    fill='black'
                    strokeWidth='0'
                    viewBox='0 0 1024 1024'
                    height='1.9em'
                    width='1.9em'
                    xmlns='http://www.w3.org/2000/svg'
                  >
                    <path d='M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm165.4 618.2l-66-.3L512 563.4l-99.3 118.4-66.1.3c-4.4 0-8-3.5-8-8 0-1.9.7-3.7 1.9-5.2l130.1-155L340.5 359a8.32 8.32 0 0 1-1.9-5.2c0-4.4 3.6-8 8-8l66.1.3L512 464.6l99.3-118.4 66-.3c4.4 0 8 3.5 8 8 0 1.9-.7 3.7-1.9 5.2L553.5 514l130 155c1.2 1.5 1.9 3.3 1.9 5.2 0 4.4-3.6 8-8 8z'></path>
                  </svg>
                </div>
              </button>
            </div>
            <div className='h-full my-4 flex flex-col justify-between px-12'>
              <div className=' w-full h-24 flex flex-row justify-between px-10'>
                <div className=' bg-gray-50 w-32'>
                  <img src={GameController} alt='컨트롤 아이콘' />
                </div>
                <div className='w-full h-full flex flex-row justify-start items-center text-3xl font-PtdBold px-10'>
                  월디에 오신 것을
                  <br /> 환영합니다.
                </div>
              </div>
              <div className=' w-full h-fit'>
                <img src={LoginLogo} alt='로그인 로고' />
              </div>
              <div className=' w-full h-fit flex justify-center items-center'>
                <KakaoLogin
                  onSuccess={handleKakaoLoginSuccess}
                  onFailure={handleKakaoLoginFailure}
                />
                {/* <button>
                  <img src={KakaoLoginButton} alt='카카오 로그인 버튼' />
                </button> */}
              </div>
              <div className=' w-full h-20 flex flex-col items-center justify-between py-4 font-PtdMedium text-base'>
                <div className='flex flex-row justify-center items-center'>
                  <div className='mr-2'>아직 WORLDY 회원이 아니신가요?</div>
                  <div className='text-blue-400 underline underline-offset-2 decoration-2'>
                    <button onClick={clickKakaoLogin}>계정 생성</button>
                    {/* <a href='/'>계정 생성</a> */}
                  </div>
                </div>
                <div className='underline underline-offset-2 decoration-2'>
                  <a href='/'>서비스 이용약관</a>
                </div>
              </div>
            </div>
          </div>
        ) : (
          <div className='w-full h-full flex flex-row justify-center items-center'>
            <LoaderPyramid text='첫 인사 생각 중...' />
          </div>
        )}
      </div>
      {showErrorModal && (
        <div className='z-30 bg-[rgba(0,0,0,0.5)] w-full h-full top-0 left-0 flex justify-center items-center fixed'>
          <div className='bg-white rounded-[10px] w-[80%] max-w-[400px] h-fit min-h-[200px] '>
            <div className='flex justify-end items-start h-1/5 px-[10px] pt-[10px]'>
              <button
                onClick={closeErrorModal}
                className='h-fit w-fit rounded-[999px]'
              >
                <svg
                  stroke='currentColor'
                  fill='currentColor'
                  stroke-width='0'
                  viewBox='0 0 1024 1024'
                  height='1.8em'
                  width='1.8em'
                  xmlns='http://www.w3.org/2000/svg'
                >
                  <path d='M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm165.4 618.2l-66-.3L512 563.4l-99.3 118.4-66.1.3c-4.4 0-8-3.5-8-8 0-1.9.7-3.7 1.9-5.2l130.1-155L340.5 359a8.32 8.32 0 0 1-1.9-5.2c0-4.4 3.6-8 8-8l66.1.3L512 464.6l99.3-118.4 66-.3c4.4 0 8 3.5 8 8 0 1.9-.7 3.7-1.9 5.2L553.5 514l130 155c1.2 1.5 1.9 3.3 1.9 5.2 0 4.4-3.6 8-8 8z'></path>
                </svg>
              </button>
            </div>
            <div className='h-3/5 mt-[20px] mb-[30px] flex justify-center items-start text-center font-PtdRegular text-[22px] '>
              요청하신 작업을 수행하지 못했습니다.
              <br />
              <br />
              잠시 후 다시 시도해주세요.
            </div>
            <div className='h-[60px] flex justify-center items-center border-t-[rgba(0,0,0,0.3)] border-t-[1px] border-solid font-PtdRegular text-[18px]'>
              <button className='h-full w-full' onClick={closeErrorModal}>
                확인
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
